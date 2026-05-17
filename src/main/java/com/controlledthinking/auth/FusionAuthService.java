package com.controlledthinking.auth;

import com.controlledthinking.db.AppUser;
import com.controlledthinking.db.Customer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FusionAuthService implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(FusionAuthService.class);

    private final String fusionAuthUrl;
    private final String applicationId;
    private final String apiKey;
    private final SessionFactory sessionFactory;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // kid → public key, refreshed on startup and on validation failure
    private final Map<String, PublicKey> jwksCache = new ConcurrentHashMap<>();

    public FusionAuthService(String fusionAuthUrl, String applicationId, String apiKey,
                              SessionFactory sessionFactory) {
        this.fusionAuthUrl = fusionAuthUrl;
        this.applicationId = applicationId;
        this.apiKey = apiKey;
        this.sessionFactory = sessionFactory;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        refreshJwks();
    }

    // -------------------------------------------------------------------------
    // AuthService — validate an incoming FusionAuth JWT
    // -------------------------------------------------------------------------

    @Override
    public Optional<User> validateToken(String token) {
        Optional<User> result = tryValidate(token);
        if (result.isPresent()) {
            return result;
        }
        // Key rotation: refresh once and retry
        refreshJwks();
        return tryValidate(token);
    }

    private Optional<User> tryValidate(String token) {
        for (PublicKey key : jwksCache.values()) {
            try {
                Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                return Optional.of(buildUser(claims));
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }

    private User buildUser(Claims claims) {
        String sub = claims.getSubject();

        @SuppressWarnings("unchecked")
        List<String> rolesList = claims.get("roles", List.class);
        Set<String> roles = rolesList == null ? Set.of() : Set.copyOf(rolesList);

        String email = claims.get("email", String.class);
        String name = email != null ? email : sub;

        UUID customerId = lookupCustomerId(sub);
        if (customerId == null) {
            // First login — provision a local AppUser and Customer
            User created = findOrCreateUser("fusionauth", sub, email, email);
            customerId = created.getCustomerId();
        }

        return new User(name, roles, customerId);
    }

    // -------------------------------------------------------------------------
    // AuthService — find or create the local AppUser on first login
    // -------------------------------------------------------------------------

    @Override
    public User findOrCreateUser(String provider, String subjectId,
                                  String email, String preferredUsername) {
        try (Session session = sessionFactory.openSession()) {
            Optional<AppUser> existing = session
                .createNamedQuery("AppUser.findByOAuthSubject", AppUser.class)
                .setParameter("provider", provider)
                .setParameter("subjectId", subjectId)
                .uniqueResultOptional();

            if (existing.isPresent()) {
                return toUser(existing.get());
            }

            Transaction tx = session.beginTransaction();
            try {
                Customer customer = new Customer();
                customer.setCustomerId(UUID.randomUUID());
                customer.setName(preferredUsername != null ? preferredUsername : email);
                customer.setCreditBalance(new BigDecimal("1.00"));
                session.persist(customer);

                AppUser newUser = new AppUser();
                newUser.setUsername(preferredUsername != null ? preferredUsername : email);
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
    // AuthService — log in a user via FusionAuth and return the JWT
    // -------------------------------------------------------------------------

    @Override
    public String loginUser(String email, String password) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                "loginId", email,
                "password", password,
                "applicationId", applicationId
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fusionAuthUrl + "/api/login"))
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status == 404 || status == 401) {
                logger.warn("FusionAuth login rejected: HTTP {} — {}", status, response.body());
                throw new RuntimeException("Invalid credentials");
            }
            // 200: success, 202: success (password change required), 203: success (email verification pending)
            if (status != 200 && status != 202 && status != 203) {
                logger.error("FusionAuth login failed: HTTP {} — {}", status, response.body());
                throw new RuntimeException("Login failed");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String token = json.path("token").asText(null);
            if (token == null || token.isBlank()) {
                throw new RuntimeException("No token in FusionAuth login response");
            }
            return token;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Login failed", e);
        }
    }

    // -------------------------------------------------------------------------
    // AuthService — register a new user in FusionAuth
    // -------------------------------------------------------------------------

    @Override
    public User registerUser(String email, String password) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                "registration", Map.of("applicationId", applicationId),
                "user", Map.of(
                    "email", email,
                    "password", password
                )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fusionAuthUrl + "/api/user/registration"))
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 400) {
                JsonNode json = objectMapper.readTree(response.body());
                JsonNode fieldErrors = json.path("fieldErrors");
                if (fieldErrors.has("user.password")) {
                    throw new RuntimeException("Password does not meet requirements");
                }
                logger.error("FusionAuth registration bad request: {}", response.body());
                throw new RuntimeException("Registration failed");
            }

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                logger.error("FusionAuth registration failed: HTTP {} — {}", response.statusCode(), response.body());
                throw new RuntimeException("Registration failed");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String subjectId = json.path("user").path("id").asText();

            return findOrCreateUser("fusionauth", subjectId, email, email);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Registration failed", e);
        }
    }

    // -------------------------------------------------------------------------
    // JWKS management
    // -------------------------------------------------------------------------

    private void refreshJwks() {
        try {
            String jwksUrl = fusionAuthUrl + "/.well-known/jwks.json";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jwksUrl))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("JWKS fetch failed: HTTP {}", response.statusCode());
                return;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode keys = root.get("keys");
            if (keys == null || !keys.isArray()) {
                logger.error("JWKS response missing keys array");
                return;
            }

            Map<String, PublicKey> newCache = new ConcurrentHashMap<>();
            for (JsonNode keyNode : keys) {
                if (!"RSA".equals(keyNode.path("kty").asText())) {
                    continue;
                }
                String kid = keyNode.path("kid").asText("default");
                String n   = keyNode.path("n").asText();
                String e   = keyNode.path("e").asText();
                try {
                    newCache.put(kid, buildRsaPublicKey(n, e));
                } catch (Exception ex) {
                    logger.warn("Could not parse JWKS key kid={}", kid, ex);
                }
            }

            jwksCache.clear();
            jwksCache.putAll(newCache);
            logger.info("JWKS refreshed: {} key(s) loaded", jwksCache.size());

        } catch (Exception e) {
            logger.error("JWKS refresh failed", e);
        }
    }

    private static PublicKey buildRsaPublicKey(String n, String e) throws Exception {
        BigInteger modulus  = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
        return KeyFactory.getInstance("RSA")
            .generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UUID lookupCustomerId(String fusionAuthUserId) {
        try (Session session = sessionFactory.openSession()) {
            return session
                .createNamedQuery("AppUser.findByOAuthSubject", AppUser.class)
                .setParameter("provider", "fusionauth")
                .setParameter("subjectId", fusionAuthUserId)
                .uniqueResultOptional()
                .map(u -> u.getCustomer() != null ? u.getCustomer().getCustomerId() : null)
                .orElse(null);
        }
    }

    private User toUser(AppUser appUser) {
        UUID customerId = appUser.getCustomer() != null
            ? appUser.getCustomer().getCustomerId()
            : null;
        String name = appUser.getEmail() != null ? appUser.getEmail() : appUser.getUsername();
        return new User(name, Set.of("USER"), customerId);
    }
}
