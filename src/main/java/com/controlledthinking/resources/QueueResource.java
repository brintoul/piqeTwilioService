package com.controlledthinking.resources;

import com.controlledthinking.api.Notification;
import com.controlledthinking.auth.User;
import com.controlledthinking.client.TwilioServicesProvider;
import com.controlledthinking.dao.AlertQueueDAO;
import com.controlledthinking.dao.AlertQueueMessageDAO;
import com.controlledthinking.db.AlertMessage;
import com.controlledthinking.db.AlertQueue;
import com.controlledthinking.service.MessageResultProcessor;
import com.controlledthinking.dto.AlertQueuePieces;
import com.twilio.rest.api.v2010.account.Message;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brintoul
 */
@Path("/queue")
public class QueueResource {

    private static final Logger logger = LoggerFactory.getLogger(QueueResource.class);
    private final AlertQueueDAO aqDao;
    private final AlertQueueMessageDAO aqmDao;
    private final TwilioServicesProvider twilio;
    private final MessageResultProcessor messageResultProcessor;
    private final BigDecimal costPerMessage;
    private final BigDecimal lowCreditThreshold;

    public QueueResource(TwilioServicesProvider twilioClient, AlertQueueDAO aqDao, AlertQueueMessageDAO aqmDao, MessageResultProcessor messageResultProcessor, BigDecimal costPerMessage, BigDecimal lowCreditThreshold) {
        this.twilio = twilioClient;
        this.aqDao = aqDao;
        this.aqmDao = aqmDao;
        this.messageResultProcessor = messageResultProcessor;
        this.costPerMessage = costPerMessage;
        this.lowCreditThreshold = lowCreditThreshold;
    }

    @GET
    @UnitOfWork
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyQueues(@Auth User user) {
        List<AlertQueue> queues = aqDao.findQueuesByCustomer(user.getCustomerId().toString());
        logger.debug("Found {} queues for customer {}", queues.size(), user.getCustomerId());
        return Response.ok(queues).build();
    }

    @POST
    @UnitOfWork(transactional = true)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createQueue(@Valid AlertQueue queue, @Auth User user) {
        queue.setCustomerId(user.getCustomerId());
        if (aqDao.countQueuesByCustomer(user.getCustomerId().toString()) == 0) {
            queue.setCurrentQueue(true);
        }
        AlertQueue created = aqDao.createQueue(queue);
        logger.info("Created new queue {} for customer {} (current={})", created.getQueueId(), created.getCustomerId(), created.getCurrentQueue());
        return Response.ok(created).build();
    }

    @Path("/{id}")
    @GET
    @UnitOfWork
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQueueEntries(@PathParam("id") String queueId) {

        List<AlertQueuePieces> pieces = aqDao.findAlertPieces(queueId);
        logger.debug("The pieces size is: {}", pieces.size());
        return Response.ok(pieces).build();
    }

    @Path("/{id}")
    @DELETE
    @UnitOfWork(transactional = true)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteQueue(@PathParam("id") String queueId, @Auth User user) {
        aqDao.deleteQueue(queueId);
        logger.info("Deleted queue {} for customer {}", queueId, user.getCustomerId());
        List<AlertQueue> queues = aqDao.findQueuesByCustomer(user.getCustomerId().toString());
        return Response.ok(queues).build();
    }

    @Path("/{id}/{order}")
    @DELETE
    @UnitOfWork(transactional = true)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteQueueEntry(@PathParam("id") String queueId,
            @PathParam("order") int order) {

        aqDao.deleteQueueEntry(queueId, order);
        logger.info("We deleted entry {} from queue {}", order, queueId);
        List<AlertQueuePieces> pieces = aqDao.findAlertPieces(queueId);
        return Response.ok(pieces).build();
    }

    @Path("/{id}/alert")
    @POST
    @UnitOfWork(transactional = true)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response alertQueueMember(@PathParam("id") String queueId, @Valid Notification notification, @Auth User user) {

        logger.info("We are going to alert {} from queue {}", notification.getPhoneNumber(), queueId);
        logger.debug("The message is: " + notification.getMessageBody());
        aqDao.setAlerted(queueId, notification.getPhoneNumber());
        String from = "+18583566213";
        Message m = twilio.sendSms(from, notification.getPhoneNumber(), notification.getMessageBody());
        messageResultProcessor.processSentMessageResult(m, user.getCustomerId(), costPerMessage);

        BigDecimal balance = aqDao.getCustomerBalance(user.getCustomerId());
        boolean lowCredits = balance.subtract(costPerMessage).compareTo(lowCreditThreshold) < 0;
        logger.info("Post-alert balance for customer {}: {} (lowCredits={})", user.getCustomerId(), balance, lowCredits);

        return Response.ok(Map.of("lowCredits", lowCredits)).build();
    }

    @Path("/{id}/message")
    @PUT
    @UnitOfWork
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveMessage(@PathParam("id") String queueId, @NotNull @Valid AlertMessage message) {
        aqmDao.createMessage(message, UUID.fromString(queueId));
        logger.info("New queue message saved for queue {}", queueId);
        return Response.ok().entity("Saved message: " + "").build();
    }

    @Path("/{id}/messages")
    @GET
    @UnitOfWork
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQueueMessages(@PathParam("id") String queueId) {
        List<AlertMessage> messages = aqmDao.fetchAllQueueMessages(queueId);
        logger.debug("Found {} messages for queue {}", messages.size(), queueId);
        return Response.ok(messages).build();
    }

    @Path("/{id}/messages/{messageId}")
    @DELETE
    @UnitOfWork(transactional = true)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMessage(@PathParam("id") String queueId,
            @PathParam("messageId") Integer messageId) {
        aqmDao.deleteMessage(messageId);
        logger.info("Deleted message {} from queue {}", messageId, queueId);
        List<AlertMessage> messages = aqmDao.fetchAllQueueMessages(queueId);
        return Response.ok(messages).build();
    }

    @Path("/{id}/messages/{messageId}/active")
    @PUT
    @UnitOfWork(transactional = true)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setActiveMessage(@PathParam("id") String queueId,
            @PathParam("messageId") Integer messageId) {
        aqmDao.setActiveMessage(queueId, messageId);
        logger.info("Set message {} as active for queue {}", messageId, queueId);
        List<AlertMessage> messages = aqmDao.fetchAllQueueMessages(queueId);
        return Response.ok(messages).build();
    }

    @Path("/{id}/current")
    @PUT
    @UnitOfWork(transactional = true)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setCurrentQueue(@PathParam("id") String queueId) {
        aqDao.setCurrentQueue(queueId);
        logger.info("Set queue {} as current queue", queueId);
        return Response.ok().build();
    }
}