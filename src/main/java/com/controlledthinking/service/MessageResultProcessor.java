/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.controlledthinking.service;

import com.twilio.rest.api.v2010.account.Message;
import java.math.BigDecimal;
import java.util.UUID;

/**
 *
 * @author brintoul
 */
public interface MessageResultProcessor {
    boolean processSentMessageResult(Message m, UUID customerId, BigDecimal cost);
}
