package com.controlledthinking.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class JwtUtil {

    private static final long EXPIRY_MS = 24 * 60 * 60 * 1000L; // 24 hours

    private final SecretKey key;

    public JwtUtil(String secret) {
        // Key must be at least 32 bytes for HMAC-SHA256
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        return Jwts.builder()
            .subject(user.getName())
            .claim("roles", user.getRoles())
            .claim("customerId", user.getCustomerId() != null ? user.getCustomerId().toString() : null)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
            .signWith(key)
            .compact();
    }

    public Optional<User> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String username = claims.getSubject();
            @SuppressWarnings("unchecked")
            List<String> rolesList = claims.get("roles", List.class);
            Set<String> roles = rolesList == null ? Set.of() : new HashSet<>(rolesList);

            String customerIdStr = claims.get("customerId", String.class);
            UUID customerId = customerIdStr != null ? UUID.fromString(customerIdStr) : null;

            return Optional.of(new User(username, roles, customerId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
