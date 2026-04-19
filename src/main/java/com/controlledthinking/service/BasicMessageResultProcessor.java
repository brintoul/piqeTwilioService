/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.controlledthinking.service;

import com.controlledthinking.dao.CustomerTransactionDAO;
import com.controlledthinking.db.Customer;
import com.twilio.rest.api.v2010.account.Message;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brintoul
 */
public class BasicMessageResultProcessor implements MessageResultProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BasicMessageResultProcessor.class);

    private final CustomerTransactionDAO ctDao;
    private final boolean devMode;
    private final SessionFactory sessionFactory;

    public BasicMessageResultProcessor(CustomerTransactionDAO ctDao, boolean devMode, SessionFactory sessionFactory) {
        this.ctDao = ctDao;
        this.devMode = devMode;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public boolean processSentMessageResult(Message m, UUID customerId, BigDecimal cost) {
        ctDao.createTransaction(m, customerId, cost);
        if (devMode) {
            deductCreditImmediately(customerId, cost);
        }
        return true;
    }

    private void deductCreditImmediately(UUID customerId, BigDecimal cost) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            Customer customer = session.get(Customer.class, customerId);
            if (customer == null) {
                logger.warn("[devMode] No customer found for ID: {}", customerId);
                tx.rollback();
                return;
            }
            BigDecimal oldBalance = customer.getCreditBalance();
            BigDecimal newBalance = oldBalance.subtract(cost);
            customer.setCreditBalance(newBalance);
            session.merge(customer);
            tx.commit();
            logger.info("[devMode] Deducted {} from customer '{}': {} -> {}", cost, customer.getName(), oldBalance, newBalance);
        } catch (Exception e) {
            logger.error("[devMode] Failed to deduct credit for customer ID: {}", customerId, e);
        }
    }
}
