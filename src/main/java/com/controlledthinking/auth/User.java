package com.controlledthinking.auth;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

public class User implements Principal {

    private final String name;
    private final Set<String> roles;
    private final UUID customerId;

    public User(String name) {
        this(name, Set.of(), null);
    }

    public User(String name, Set<String> roles) {
        this(name, roles, null);
    }

    public User(String name, Set<String> roles, UUID customerId) {
        this.name = name;
        this.roles = roles;
        this.customerId = customerId;
    }

    @Override
    public String getName() {
        return name;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public UUID getCustomerId() {
        return customerId;
    }
}
