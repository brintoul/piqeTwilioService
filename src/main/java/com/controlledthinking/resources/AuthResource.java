package com.controlledthinking.resources;

import com.controlledthinking.auth.User;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @GET
    @Path("/login")
    public Response login(@Auth User user) {
        return Response.ok(Map.of(
            "username", user.getName(),
            "roles", user.getRoles(),
            "customerId", user.getCustomerId().toString()
        )).build();
    }
}
