package com.controlledthinking.resources;

import com.controlledthinking.db.Customer;
import com.controlledthinking.db.CustomerTransaction;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

@Path("/sms")
public class SmsWebhookResource {

    private static final Logger logger = LoggerFactory.getLogger(SmsWebhookResource.class);

    private final SessionFactory sessionFactory;

    public SmsWebhookResource(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @POST
    @Path("/incoming")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response receiveIncomingSms(
            @FormParam("From") String from,
            @FormParam("To") String to,
            @FormParam("Body") String body,
            @FormParam("MessageSid") String messageSid,
            @FormParam("AccountSid") String accountSid,
            @FormParam("NumMedia") String numMedia) {

        logger.info("Incoming SMS received - From: {}, To: {}, MessageSid: {}", from, to, messageSid);
        logger.info("Message body: {}", body);

        if (numMedia != null && !numMedia.equals("0")) {
            logger.info("Message contains {} media items", numMedia);
        }

        String twiml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>";
        return Response.ok(twiml).build();
    }

    @POST
    @Path("/status")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response receiveStatusCallback(
            @FormParam("MessageSid") String messageSid,
            @FormParam("MessageStatus") String messageStatus,
            @FormParam("To") String to,
            @FormParam("From") String from,
            @FormParam("AccountSid") String accountSid,
            @FormParam("ErrorCode") String errorCode,
            @FormParam("ErrorMessage") String errorMessage) {

        logger.info("Status callback - MessageSid: {}, Status: {}, To: {}, From: {}", messageSid, messageStatus, to, from);

        if (errorCode != null) {
            logger.warn("Message error - Code: {}, Message: {}", errorCode, errorMessage);
        }

        if ("delivered".equals(messageStatus)) {
            deductCredit(messageSid);
        }

        return Response.ok().build();
    }

    private void deductCredit(String messageSid) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            CustomerTransaction ct = session.get(CustomerTransaction.class, messageSid);
            if (ct == null) {
                logger.warn("No transaction found for MessageSid: {}", messageSid);
                tx.rollback();
                return;
            }

            Customer customer = ct.getCustomer();
            BigDecimal oldBalance = customer.getCreditBalance();
            BigDecimal newBalance = oldBalance.subtract(ct.getCost());
            customer.setCreditBalance(newBalance);
            session.merge(customer);
            tx.commit();

            logger.info("Deducted {} from customer '{}': {} -> {}", ct.getCost(), customer.getName(), oldBalance, newBalance);
        } catch (Exception e) {
            logger.error("Failed to deduct credit for MessageSid: {}", messageSid, e);
        }
    }
}
