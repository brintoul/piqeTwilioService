package com.controlledthinking.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

import java.util.Optional;

public class JwtAuthenticator implements Authenticator<String, User> {

    private final AuthService authService;

    public JwtAuthenticator(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Optional<User> authenticate(String token) throws AuthenticationException {
        return authService.validateToken(token);
    }
}
