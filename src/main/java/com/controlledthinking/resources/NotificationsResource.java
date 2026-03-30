package com.controlledthinking.resources;

import com.controlledthinking.client.TwilioServicesProvider;
import com.controlledthinking.api.Notification;
import com.controlledthinking.dao.PersonOrEntityDAO;
import com.controlledthinking.db.PersonOrEntity;
import com.twilio.rest.api.v2010.account.Message;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Optional;

@Path("/notifications")
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationsResource {

    private final TwilioServicesProvider twilio;
    private final PersonOrEntityDAO peDao;

    public NotificationsResource(TwilioServicesProvider twilio, PersonOrEntityDAO peDao) {
        this.peDao = peDao;
        this.twilio = twilio;
    }

    @POST
    @UnitOfWork
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendMessage(@NotNull @Valid Notification notification) {

        String from = "+18583566213";
        String to = "+16197649620";
        for(String peId : notification.getPersonOrEntityIds()) {
            Optional<PersonOrEntity> peOptional = peDao.findById(peId);
            to = peOptional.get().getPhoneNumber();
        }
        Message message = twilio.sendSms(from, to, notification.getMessageBody());
        return Response.ok().entity("Sent: " + message.getSid()).build();
    }

}