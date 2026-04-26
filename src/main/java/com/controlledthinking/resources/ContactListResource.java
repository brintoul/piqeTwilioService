package com.controlledthinking.resources;

import com.controlledthinking.auth.User;
import com.controlledthinking.dao.ContactListDAO;
import com.controlledthinking.db.ContactList;
import com.controlledthinking.dto.ContactListSummary;
import com.controlledthinking.service.ContactListService;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/contactList")
public class ContactListResource {

    private static final Logger logger = LoggerFactory.getLogger(ContactListResource.class);

    private final ContactListDAO clDao;
    private final ContactListService clService;

    public ContactListResource(ContactListDAO clDao, ContactListService clService) {
        this.clDao = clDao;
        this.clService = clService;
    }

    @Path("/upload")
    @POST
    @UnitOfWork
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@Auth User user,
                           @FormDataParam("name") String name,
                           @FormDataParam("file") InputStream fileStream) {
        if (fileStream == null) {
            return Response.status(400).entity("File part is missing").type(MediaType.TEXT_PLAIN).build();
        }
        if (name == null || name.isBlank()) {
            return Response.status(400).entity("Contact list name is required").type(MediaType.TEXT_PLAIN).build();
        }
        try {
            ContactList list = clService.createFromUpload(name, fileStream, user.getCustomerId());
            return Response.ok(list).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        } catch (IOException e) {
            logger.error("Failed to process contact list upload: {}", e.getMessage());
            return Response.serverError().entity("Failed to process uploaded file").type(MediaType.TEXT_PLAIN).build();
        }
    }

    @Path("/all")
    @GET
    @UnitOfWork
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(@Auth User user) {
        List<ContactListSummary> summaries = clDao.findSummariesByCustomerId(user.getCustomerId());
        return Response.ok(summaries).build();
    }

    @Path("/{id}/contacts")
    @GET
    @UnitOfWork
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContacts(@Auth User user, @PathParam("id") String id) {
        return clDao.findByIdAndCustomerId(id, user.getCustomerId())
            .map(list -> Response.ok(list.getPeople()).build())
            .orElse(Response.status(404).build());
    }

    @Path("/{listId}/contacts/{personId}")
    @DELETE
    @UnitOfWork
    public Response removeContact(@Auth User user,
                                  @PathParam("listId") String listId,
                                  @PathParam("personId") String personId) {
        boolean removed = clDao.removeContact(listId, personId, user.getCustomerId());
        return removed ? Response.noContent().build() : Response.status(404).build();
    }
}
