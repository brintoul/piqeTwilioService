package com.controlledthinking.resources;

import com.controlledthinking.TwilioPIQEConfiguration;
import com.controlledthinking.TwilioPIQEConfiguration.OAuthProviderConfig;
import com.controlledthinking.auth.JwtUtil;
import com.controlledthinking.auth.User;
import com.controlledthinking.db.AppUser;
import com.controlledthinking.db.Customer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Path("/auth/oauth")
public class OAuthResource {

    private static final Logger logger = LoggerFactory.getLogger(OAuthResource.class);

    private final TwilioPIQEConfiguration config;
    private final SessionFactory sessionFactory;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public OAuthResource(TwilioPIQEConfiguration config,
                         SessionFactory sessionFactory,
                         JwtUtil jwtUtil) {
        this.config = config;
        this.sessionFactory = sessionFactory;
        this.jwtUtil = jwtUtil;
    }

    // -------------------------------------------------------------------------
    // Step 1: initiate — redirect user's browser to the provider's auth page
    // -------------------------------------------------------------------------
    @GET
    @Path("/{provider}")
    public Response initiate(@PathParam("provider") String provider) {
        OAuthProviderConfig providerConfig = getProviderConfig(provider);
        if (providerConfig == null || !providerConfig.isConfigured()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("OAuth provider '" + provider + "' is not configured")
                .build();
        }

        String authUrl = buildAuthUrl(provider, providerConfig);
        return Response.temporaryRedirect(URI.create(authUrl)).build();
    }

    // -------------------------------------------------------------------------
    // Step 2: callback — exchange code for token, find/create user, issue JWT
    // -------------------------------------------------------------------------
    @GET
    @Path("/{provider}/callback")
    public Response callback(
            @PathParam("provider") String provider,
            @QueryParam("code") String code,
            @QueryParam("error") String error) {

        String frontendBase = config.getFrontendBaseUrl();

        if (error != null || code == null) {
            logger.warn("OAuth callback error for provider {}: {}", provider, error);
            return Response.temporaryRedirect(
                URI.create(frontendBase + "/?error=oauth_denied")).build();
        }

        OAuthProviderConfig providerConfig = getProviderConfig(provider);
        if (providerConfig == null || !providerConfig.isConfigured()) {
            return Response.temporaryRedirect(
                URI.create(frontendBase + "/?error=provider_not_configured")).build();
        }

        try {
            // Exchange code for access token
            String accessToken = exchangeCodeForToken(provider, providerConfig, code);
            if (accessToken == null) {
                logger.error("Failed to exchange code for token with provider {}", provider);
                return Response.temporaryRedirect(
                    URI.create(frontendBase + "/?error=token_exchange_failed")).build();
            }

            // Fetch user info from provider
            Map<String, Object> userInfo = fetchUserInfo(provider, accessToken);
            if (userInfo == null) {
                logger.error("Failed to fetch user info from provider {}", provider);
                return Response.temporaryRedirect(
                    URI.create(frontendBase + "/?error=userinfo_failed")).build();
            }

            // Extract provider-specific fields
            String subjectId  = extractSubjectId(provider, userInfo);
            String email      = extractEmail(provider, userInfo);
            String preferredUsername = extractUsername(provider, userInfo);

            // Find or create user
            User user = findOrCreateUser(provider, subjectId, email, preferredUsername);

            // Generate JWT
            String jwt = jwtUtil.generateToken(user);

            // Build roles as JSON array string for the frontend
            String rolesJson = "[" + String.join(",",
                user.getRoles().stream().map(r -> "\"" + r + "\"").toArray(String[]::new)) + "]";

            String redirectUrl = frontendBase + "/auth/callback"
                + "?token=" + urlEncode(jwt)
                + "&username=" + urlEncode(user.getName())
                + "&roles=" + urlEncode(rolesJson)
                + "&customerId=" + urlEncode(user.getCustomerId() != null
                    ? user.getCustomerId().toString() : "");

            return Response.temporaryRedirect(URI.create(redirectUrl)).build();

        } catch (Exception e) {
            logger.error("OAuth callback failed for provider {}", provider, e);
            return Response.temporaryRedirect(
                URI.create(frontendBase + "/?error=oauth_failed")).build();
        }
    }

    // -------------------------------------------------------------------------
    // Provider config lookup
    // -------------------------------------------------------------------------
    private OAuthProviderConfig getProviderConfig(String provider) {
        return switch (provider.toLowerCase()) {
            case "google"    -> config.getGoogle();
            case "microsoft" -> config.getMicrosoft();
            case "github"    -> config.getGithub();
            default          -> null;
        };
    }

    // -------------------------------------------------------------------------
    // Build the provider authorization URL
    // -------------------------------------------------------------------------
    private String buildAuthUrl(String provider, OAuthProviderConfig cfg) {
        String baseUrl;
        String scope;
        switch (provider.toLowerCase()) {
            case "google":
                baseUrl = "https://accounts.google.com/o/oauth2/v2/auth";
                scope   = "openid email profile";
                break;
            case "microsoft":
                baseUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
                scope   = "openid email profile";
                break;
            case "github":
                baseUrl = "https://github.com/login/oauth/authorize";
                scope   = "user:email";
                break;
            default:
                throw new IllegalArgumentException("Unknown provider: " + provider);
        }

        return baseUrl
            + "?client_id="     + urlEncode(cfg.getClientId())
            + "&redirect_uri="  + urlEncode(cfg.getCallbackUrl())
            + "&response_type=code"
            + "&scope="         + urlEncode(scope);
    }

    // -------------------------------------------------------------------------
    // Exchange authorization code for access token
    // -------------------------------------------------------------------------
    private String exchangeCodeForToken(String provider, OAuthProviderConfig cfg, String code)
            throws Exception {

        String tokenUrl;
        switch (provider.toLowerCase()) {
            case "google":
                tokenUrl = "https://oauth2.googleapis.com/token";
                break;
            case "microsoft":
                tokenUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
                break;
            case "github":
                tokenUrl = "https://github.com/login/oauth/access_token";
                break;
            default:
                return null;
        }

        String body = "grant_type=authorization_code"
            + "&code="          + urlEncode(code)
            + "&client_id="     + urlEncode(cfg.getClientId())
            + "&client_secret=" + urlEncode(cfg.getClientSecret())
            + "&redirect_uri="  + urlEncode(cfg.getCallbackUrl());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.error("Token exchange failed: HTTP {} — {}", response.statusCode(), response.body());
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> json = objectMapper.readValue(response.body(), Map.class);
        Object token = json.get("access_token");
        return token != null ? token.toString() : null;
    }

    // -------------------------------------------------------------------------
    // Fetch user profile from the provider's userinfo endpoint
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchUserInfo(String provider, String accessToken)
            throws Exception {

        String userInfoUrl;
        switch (provider.toLowerCase()) {
            case "google":
                userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
                break;
            case "microsoft":
                userInfoUrl = "https://graph.microsoft.com/v1.0/me";
                break;
            case "github":
                userInfoUrl = "https://api.github.com/user";
                break;
            default:
                return null;
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(userInfoUrl))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.error("UserInfo fetch failed: HTTP {} — {}", response.statusCode(), response.body());
            return null;
        }

        return objectMapper.readValue(response.body(), Map.class);
    }

    // -------------------------------------------------------------------------
    // Extract the stable unique subject ID from each provider's userinfo
    // -------------------------------------------------------------------------
    private String extractSubjectId(String provider, Map<String, Object> userInfo) {
        return switch (provider.toLowerCase()) {
            case "google"    -> str(userInfo.get("sub"));
            case "microsoft" -> str(userInfo.get("id"));
            case "github"    -> str(userInfo.get("id"));  // integer in JSON, toString it
            default          -> null;
        };
    }

    private String extractEmail(String provider, Map<String, Object> userInfo) {
        return switch (provider.toLowerCase()) {
            case "google"    -> str(userInfo.get("email"));
            case "microsoft" -> {
                String mail = str(userInfo.get("mail"));
                yield mail != null ? mail : str(userInfo.get("userPrincipalName"));
            }
            case "github"    -> str(userInfo.get("email"));
            default          -> null;
        };
    }

    private String extractUsername(String provider, Map<String, Object> userInfo) {
        return switch (provider.toLowerCase()) {
            case "google"    -> str(userInfo.get("email"));
            case "microsoft" -> {
                String upn = str(userInfo.get("userPrincipalName"));
                yield upn != null ? upn : str(userInfo.get("mail"));
            }
            case "github"    -> str(userInfo.get("login"));
            default          -> null;
        };
    }

    // -------------------------------------------------------------------------
    // Find existing user by OAuth subject, or create a new one
    // -------------------------------------------------------------------------
    private User findOrCreateUser(String provider, String subjectId,
                                   String email, String preferredUsername) {
        try (Session session = sessionFactory.openSession()) {
            // Check for existing OAuth user
            Optional<AppUser> existing = session
                .createNamedQuery("AppUser.findByOAuthSubject", AppUser.class)
                .setParameter("provider", provider)
                .setParameter("subjectId", subjectId)
                .uniqueResultOptional();

            if (existing.isPresent()) {
                AppUser appUser = existing.get();
                return toUser(appUser);
            }

            // New OAuth user — create Customer + AppUser in a transaction
            Transaction tx = session.beginTransaction();
            try {
                Customer customer = new Customer();
                customer.setCustomerId(UUID.randomUUID());
                customer.setName(preferredUsername != null ? preferredUsername : email);
                customer.setCreditBalance(new BigDecimal("1.00"));
                session.persist(customer);

                // Ensure username uniqueness
                String username = resolveUniqueUsername(session, preferredUsername, provider, subjectId);

                AppUser newUser = new AppUser();
                newUser.setUsername(username);
                newUser.setEmail(email);
                newUser.setOauthProvider(provider);
                newUser.setOauthSubjectId(subjectId);
                newUser.setCustomer(customer);
                session.persist(newUser);

                tx.commit();
                return toUser(newUser);

            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        }
    }

    // -------------------------------------------------------------------------
    // If the preferred username is already taken, fall back to provider_subjectId
    // -------------------------------------------------------------------------
    private String resolveUniqueUsername(Session session, String preferred,
                                          String provider, String subjectId) {
        if (preferred != null) {
            Optional<AppUser> clash = session
                .createNamedQuery("AppUser.findByUsername", AppUser.class)
                .setParameter("username", preferred)
                .uniqueResultOptional();
            if (clash.isEmpty()) {
                return preferred;
            }
        }
        // Fallback: provider_first8charsOfSubject (guaranteed unique by DB constraint)
        return provider + "_" + subjectId.substring(0, Math.min(8, subjectId.length()));
    }

    private User toUser(AppUser appUser) {
        UUID customerId = appUser.getCustomer() != null
            ? appUser.getCustomer().getCustomerId()
            : null;
        return new User(appUser.getUsername(), Set.of("USER"), customerId);
    }

    private static String str(Object val) {
        return val != null ? val.toString() : null;
    }

    private static String urlEncode(String s) {
        if (s == null) return "";
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
