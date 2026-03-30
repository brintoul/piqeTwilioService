package com.controlledthinking.resources;

import com.controlledthinking.auth.User;
import com.controlledthinking.dao.AlertQueueDAO;
import com.controlledthinking.db.AlertQueueEntry;
import com.controlledthinking.dto.AlertQueuePieces;
import com.controlledthinking.service.PersonOrEntityService;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brintoul
 */
@Path("/inputs")
public class InputResource {

    private static final Logger logger = LoggerFactory.getLogger(InputResource.class);
    private final AlertQueueDAO aqDao;
    private final PersonOrEntityService personOrEntityService;

    public InputResource(AlertQueueDAO aqDao, PersonOrEntityService personOrEntityService) {
        this.aqDao = aqDao;
        this.personOrEntityService = personOrEntityService;
    }

    @Path("/userdata")
    @POST
    @UnitOfWork
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response postFile(@Auth User user,
                               @FormDataParam("name") String name,
                               @FormDataParam("file") InputStream uploadedInputStream,
                               @FormDataParam("file") FormDataContentDisposition fileDetail) {

        if (uploadedInputStream == null) {
            return Response.status(400).entity("File part is missing").type(MediaType.TEXT_PLAIN).build();
        }

        try {
            String result = personOrEntityService.importFromSpreadsheet(uploadedInputStream, user.getCustomerId());
            return Response.ok(result).type(MediaType.TEXT_PLAIN).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        } catch (IOException e) {
            logger.error("Failed to handle file upload: {}", e.getMessage());
            return Response.serverError().entity("Failed to process uploaded file: " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }
    }

    @Path("/queue/{id}")
    @PUT
    @UnitOfWork
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putQueueEntry(@PathParam("id") String queueId, @NotNull @Valid AlertQueueEntry entry) {
        UUID queueUuid = UUID.fromString(queueId);
        logger.debug("The queue ID is " + queueId);
        aqDao.create(entry, queueUuid);
        List<AlertQueuePieces> pieces = aqDao.findAlertPieces(queueId);
        logger.debug("The pieces size is: {}", pieces.size());
        return Response.ok("Added").build();
    }
}