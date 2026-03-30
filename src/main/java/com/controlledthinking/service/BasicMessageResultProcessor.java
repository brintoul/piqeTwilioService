/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.controlledthinking.service;

import com.controlledthinking.dao.CustomerTransactionDAO;
import com.twilio.rest.api.v2010.account.Message;
import java.math.BigDecimal;
import java.util.UUID;

/**
 *
 * @author brintoul
 */
public class BasicMessageResultProcessor implements MessageResultProcessor {

    private final CustomerTransactionDAO ctDao;

    public BasicMessageResultProcessor(CustomerTransactionDAO ctDao) {
        this.ctDao = ctDao;
    }

    @Override
    public boolean processSentMessageResult(Message m, UUID customerId, BigDecimal cost) {
        ctDao.createTransaction(m, customerId, cost);
        return true;
    }

}
