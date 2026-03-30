package com.controlledthinking.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 *
 * @author brintoul
 */
@Table(name = "ALERT_MESSAGE")
@Entity
public class AlertMessage {

    @ManyToOne
    @JoinColumn(name = "queue_id")
    @JsonIgnore
    private AlertQueue queue;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Integer messageId;

    private String message;

    private Boolean active;

    public AlertQueue getQueue() {
        return queue;
    }

    public void setQueue(AlertQueue queue) {
        this.queue = queue;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
