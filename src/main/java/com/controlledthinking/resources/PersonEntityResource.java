/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.controlledthinking.resources;

import com.controlledthinking.auth.User;
import com.controlledthinking.dao.PersonOrEntityDAO;
import com.controlledthinking.db.PersonOrEntity;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.persistence.PersistenceException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 *
 * @author brintoul
 */
@Path("/personEntity")
public class PersonEntityResource {

    private final PersonOrEntityDAO peDao;

    public PersonEntityResource(PersonOrEntityDAO peDao) {
        this.peDao = peDao;
    }
    
    @Path("/create")
    @POST
    @UnitOfWork
    @Consumes(MediaType.APPLICATION_JSON)
    public Response savePersonOrEntity(@Auth User user, @NotNull @Valid PersonOrEntity input) {
        input.setCustomer(peDao.getCustomerReference(user.getCustomerId()));
        try {
            peDao.create(input);
        } catch (PersistenceException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("A person or entity with the same first name, last name, and phone number already exists.")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        return Response.ok().entity("Created").build();
    }

    @Path("/all")
    @GET
    @UnitOfWork
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllPersonOrEntity() {
        List<PersonOrEntity> returnList = peDao.queryByString("SELECT pe FROM PersonOrEntity pe");
        return Response.ok().entity(returnList).build();
    }
}
