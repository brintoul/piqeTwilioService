package com.controlledthinking.dto;

/**
 *
 * @author brintoul
 */
public class AlertQueuePieces {
    private String name;
    private String phoneNumber;
    private Boolean alerted;

    public AlertQueuePieces(String name, String phoneNumber, Boolean alerted) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.alerted = alerted;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getAlerted() {
        return alerted;
    }

    public void setAlerted(Boolean alerted) {
        this.alerted = alerted;
    }

    
}
