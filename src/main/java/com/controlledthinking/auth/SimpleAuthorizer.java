package com.controlledthinking.auth;

import io.dropwizard.auth.Authorizer;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SimpleAuthorizer implements Authorizer<User> {

    @Override
    public boolean authorize(User user, String role, @Nullable ContainerRequestContext requestContext) {
        return user.getRoles().contains(role);
    }
}
