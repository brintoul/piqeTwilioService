package com.controlledthinking.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 *
 * @author brintoul
 */
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@NamedQueries({
    @NamedQuery(
        name = "AlertQueueEntry.setAlerted",
        query = """
            UPDATE AlertQueueEntry e
            SET e.alerted = true
            WHERE e.queue.queueId = :queueId
              AND e.phoneNumber = :phoneNumber
            """
    )
})
@Table(name = "ALERT_QUEUE_ENTRY")
public class AlertQueueEntry {

    @Id
    @ManyToOne
    @JoinColumn(name = "queue_id")
    private AlertQueue queue;

    @Id
    @Column(name = "order_within_queue")
    private Integer orderWithinQueue;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    private boolean alerted = false;

    @Column(name = "alerted_at", insertable = false, updatable = true)
    private LocalDateTime alertedAt;

    public AlertQueueEntry() {}

    public AlertQueueEntry(AlertQueue queue, Integer orderWithinQueue, String name, String phoneNumber, Boolean alerted) {
        this.queue = queue;
        this.orderWithinQueue = orderWithinQueue;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.alerted = alerted;
    }

    public AlertQueue getQueue() {
        if(queue == null) {
            this.queue = new AlertQueue();
        }
        return queue;
    }

    public void setQueue(AlertQueue queue) {
        this.queue = queue;
    }

    public void setOrderWithinQueue(Integer orderWithinQueue) {
        this.orderWithinQueue = orderWithinQueue;
    }

    public Integer getOrderWithinQueue() {
        return orderWithinQueue;
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

    public boolean isAlerted() {
        return alerted;
    }

    public void setAlerted(boolean alerted) {
        this.alerted = alerted;
    }

    public LocalDateTime getAlertedAt() {
        return alertedAt;
    }

    public void setAlertedAt(LocalDateTime alertedAt) {
        this.alertedAt = alertedAt;
    }

    
}
