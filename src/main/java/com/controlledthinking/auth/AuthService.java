package com.controlledthinking.auth;

import java.util.Optional;

public interface AuthService {
    Optional<User> validateToken(String token);
    User findOrCreateUser(String provider, String subjectId, String email, String preferredUsername);
    User registerUser(String email, String password);
    String loginUser(String email, String password);
}
