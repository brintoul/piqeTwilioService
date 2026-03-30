package com.controlledthinking.db;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AlertQueueEntryId implements Serializable {

    @ManyToOne(optional = false)
    @JoinColumn(name = "queue_id", nullable = false)
    private AlertQueue queue;

    @Column(name = "order_within_queue", nullable = false)
    private Integer orderWithinQueue;

    public AlertQueueEntryId() {}

    public AlertQueueEntryId(AlertQueue queue, Integer orderWithinQueue) {
        this.queue = queue;
        this.orderWithinQueue = orderWithinQueue;
    }

    public AlertQueue getQueue() {
        return queue;
    }

    public void setQueue(AlertQueue queue) {
        this.queue = queue;
    }

    public Integer getOrderWithinQueue() {
        return orderWithinQueue;
    }

    public void setOrderWithinQueue(Integer orderWithinQueue) {
        this.orderWithinQueue = orderWithinQueue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlertQueueEntryId)) return false;
        AlertQueueEntryId that = (AlertQueueEntryId) o;
        return Objects.equals(queue, that.queue) &&
               Objects.equals(orderWithinQueue, that.orderWithinQueue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queue, orderWithinQueue);
    }
}
