package com.controlledthinking.resources;

import com.controlledthinking.auth.AuthService;
import com.controlledthinking.auth.User;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService;

    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @GET
    @Path("/login")
    public Response login(@Auth User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", user.getName());
        body.put("roles", user.getRoles());
        body.put("customerId", user.getCustomerId() != null ? user.getCustomerId().toString() : null);
        return Response.ok(body).build();
    }

    @POST
    @Path("/token")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response token(Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "email and password are required"))
                .build();
        }

        try {
            String token = authService.loginUser(email, password);
            User user = authService.validateToken(token).orElseThrow();

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getName());
            response.put("roles", user.getRoles());
            response.put("customerId", user.getCustomerId() != null ? user.getCustomerId().toString() : null);
            return Response.ok(response).build();

        } catch (RuntimeException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "Invalid email or password"))
                .build();
        }
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "email and password are required"))
                .build();
        }

        try {
            User user = authService.registerUser(email, password);
            Map<String, Object> response = new HashMap<>();
            response.put("username", user.getName());
            response.put("customerId", user.getCustomerId() != null ? user.getCustomerId().toString() : null);
            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
}
