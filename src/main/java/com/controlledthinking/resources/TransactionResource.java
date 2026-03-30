package com.controlledthinking.resources;

import com.controlledthinking.auth.User;
import com.controlledthinking.dao.CustomerTransactionDAO;
import com.controlledthinking.dto.TransactionSummary;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Path("/transactions")
@Produces(MediaType.APPLICATION_JSON)
public class TransactionResource {

    private static final Logger logger = LoggerFactory.getLogger(TransactionResource.class);
    private final CustomerTransactionDAO ctDao;

    public TransactionResource(CustomerTransactionDAO ctDao) {
        this.ctDao = ctDao;
    }

    @GET
    @UnitOfWork
    public Response getMyTransactions(@Auth User user) {
        List<TransactionSummary> transactions = ctDao.findByCustomerId(user.getCustomerId());
        logger.debug("Found {} transactions for customer {}", transactions.size(), user.getCustomerId());
        return Response.ok(transactions).build();
    }
}
