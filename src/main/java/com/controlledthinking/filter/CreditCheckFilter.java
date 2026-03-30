package com.controlledthinking.filter;

import com.controlledthinking.auth.JwtUtil;
import com.controlledthinking.auth.User;
import com.controlledthinking.db.AppUser;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class CreditCheckFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CreditCheckFilter.class);

    private final BigDecimal costPerMessage;
    private final SessionFactory sessionFactory;
    private final JwtUtil jwtUtil;

    public CreditCheckFilter(BigDecimal costPerMessage, SessionFactory sessionFactory, JwtUtil jwtUtil) {
        this.costPerMessage = costPerMessage;
        this.sessionFactory = sessionFactory;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        String path = httpRequest.getRequestURI();

        logger.info("CreditCheckFilter - Method: {}, Path: {}", method, path);

        // Only check credits on alert requests
        if (!path.endsWith("/alert")) {
            chain.doFilter(request, response);
            return;
        }

        String username = extractUsername(httpRequest);
        if (username == null) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing credentials");
            return;
        }

        try (Session session = sessionFactory.openSession()) {
            AppUser appUser = session
                .createNamedQuery("AppUser.findByUsername", AppUser.class)
                .setParameter("username", username)
                .uniqueResult();

            if (appUser == null) {
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unknown user");
                return;
            }

            BigDecimal creditBalance = appUser.getCustomer().getCreditBalance();
            logger.info("Credit check for user '{}': balance={}, cost={}", username, creditBalance, costPerMessage);

            if (creditBalance.compareTo(costPerMessage) < 0) {
                logger.warn("Insufficient credits for user '{}': balance={}, required={}", username, creditBalance, costPerMessage);
                httpResponse.sendError(HttpServletResponse.SC_PAYMENT_REQUIRED, "Insufficient credits");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String extractUsername(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            return null;
        }
        if (authHeader.startsWith("Basic ")) {
            String decoded = new String(Base64.getDecoder().decode(authHeader.substring(6)), StandardCharsets.UTF_8);
            return decoded.split(":", 2)[0];
        }
        if (authHeader.startsWith("Bearer ")) {
            Optional<User> user = jwtUtil.parseToken(authHeader.substring(7));
            return user.map(User::getName).orElse(null);
        }
        return null;
    }
}
