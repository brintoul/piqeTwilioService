package com.controlledthinking.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

import java.util.Optional;

public class JwtAuthenticator implements Authenticator<String, User> {

    private final JwtUtil jwtUtil;

    public JwtAuthenticator(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Optional<User> authenticate(String token) throws AuthenticationException {
        return jwtUtil.parseToken(token);
    }
}
