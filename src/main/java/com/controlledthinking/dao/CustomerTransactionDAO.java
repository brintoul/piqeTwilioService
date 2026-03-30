package com.controlledthinking.dao;

import com.controlledthinking.db.Customer;
import com.controlledthinking.db.CustomerTransaction;
import com.controlledthinking.dto.TransactionSummary;
import com.twilio.rest.api.v2010.account.Message;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerTransactionDAO extends AbstractDAO<CustomerTransaction> {

    private static final Logger logger = LoggerFactory.getLogger(CustomerTransactionDAO.class);

    public CustomerTransactionDAO(SessionFactory factory) {
        super(factory);
    }

    public void createTransaction(Message message, UUID customerId, BigDecimal cost) {
        logger.debug("Creating a customer transaction with customer ID of {}", customerId);
        logger.debug("The price is {} and the result is {}", message.getPrice(), message.getStatus());
        Customer customer = currentSession().byId(Customer.class).getReference(customerId);
        CustomerTransaction transaction = new CustomerTransaction();
        transaction.setTransactionId(message.getSid());
        transaction.setCustomer(customer);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setCost(cost);
        persist(transaction);
    }

    public List<TransactionSummary> findByCustomerId(UUID customerId) {
        return currentSession()
            .createQuery(
                "SELECT new com.controlledthinking.dto.TransactionSummary(t.transactionId, t.createdAt) " +
                "FROM CustomerTransaction t WHERE t.customer.customerId = :customerId ORDER BY t.createdAt DESC",
                TransactionSummary.class)
            .setParameter("customerId", customerId)
            .getResultList();
    }
}
