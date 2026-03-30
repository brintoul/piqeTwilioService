package com.controlledthinking.resources;

import com.controlledthinking.api.MmsNotification;
import com.controlledthinking.auth.User;
import com.controlledthinking.client.TwilioServicesProvider;
import com.controlledthinking.dao.PersonOrEntityDAO;
import com.controlledthinking.db.PersonOrEntity;
import com.controlledthinking.service.MessageResultProcessor;
import com.twilio.rest.api.v2010.account.Message;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/mms")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.TEXT_PLAIN)
public class MmsResource {

    private static final Logger logger = LoggerFactory.getLogger(MmsResource.class);

    private static final String FROM = "+18583566213";

    private final TwilioServicesProvider twilio;
    private final PersonOrEntityDAO peDao;
    private final MessageResultProcessor messageResultProcessor;
    private final String mediaBaseUrl;
    private final BigDecimal costPerMms;

    public MmsResource(TwilioServicesProvider twilio, PersonOrEntityDAO peDao,
                       MessageResultProcessor messageResultProcessor,
                       String mediaBaseUrl, BigDecimal costPerMms) {
        this.twilio = twilio;
        this.peDao = peDao;
        this.messageResultProcessor = messageResultProcessor;
        this.mediaBaseUrl = mediaBaseUrl;
        this.costPerMms = costPerMms;
    }

    @POST
    @Path("/send")
    @UnitOfWork
    public Response sendMms(@Auth User user, @NotNull @Valid MmsNotification notification) {
        String mediaUrl = mediaBaseUrl + "/" + notification.getMediaId();
        int sent = 0;

        for (String peId : notification.getPersonOrEntityIds()) {
            Optional<PersonOrEntity> pe = peDao.findById(peId);
            if (pe.isEmpty()) {
                logger.warn("Person or entity not found, skipping: {}", peId);
                continue;
            }
            Message m = twilio.sendMms(FROM, pe.get().getPhoneNumber(), notification.getMessageBody(), mediaUrl);
            messageResultProcessor.processSentMessageResult(m, user.getCustomerId(), costPerMms);
            sent++;
        }

        return Response.ok(String.format("MMS sent to %d recipient(s)", sent)).build();
    }
}
