package com.controlledthinking.resources;

import com.controlledthinking.auth.User;
import com.controlledthinking.db.Customer;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hibernate.SessionFactory;

import java.util.Map;

@Path("/credits")
@Produces(MediaType.APPLICATION_JSON)
public class CreditResource {

    private final SessionFactory sessionFactory;

    public CreditResource(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @GET
    @UnitOfWork
    public Response getCreditBalance(@Auth User user) {
        Customer customer = sessionFactory.getCurrentSession()
            .get(Customer.class, user.getCustomerId());

        if (customer == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(Map.of(
            "creditBalance", customer.getCreditBalance(),
            "customerName", customer.getName()
        )).build();
    }
}
