package com.controlledthinking.api;

import java.util.List;

/**
 *
 * @author brintoul
 */
public class Notification {

    private String messageBody;
    private List<String> personOrEntityIds;
    private String phoneNumber; //this could be used in the case of queues
    private String customerId;

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public List<String> getPersonOrEntityIds() {
        return personOrEntityIds;
    }

    public void setPersonOrEntityIds(List<String> personOrEntityIds) {
        this.personOrEntityIds = personOrEntityIds;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
}
