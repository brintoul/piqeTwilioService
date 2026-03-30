package com.controlledthinking.db;

/**
 *
 * @author brintoul
 */
import com.controlledthinking.util.UUIDCharConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.UUID;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@NamedQueries({
    @NamedQuery(
        name = "AlertQueue.findByQueueId",
        query = """
                SELECT new com.controlledthinking.dto.AlertQueuePieces(e.name, e.phoneNumber, e.alerted) 
                FROM AlertQueueEntry e 
                WHERE e.queue.queueId = :queueId ORDER BY e.orderWithinQueue"""
    )
})
@Table(name = "ALERT_QUEUE")
public class AlertQueue {

    @Id
    @Column(name = "queue_id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    //@Convert(converter = UUIDCharConverter.class)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID queueId;

    @Column(name = "customer_id", columnDefinition = "CHAR(36)", nullable = false)
    @Convert(converter = UUIDCharConverter.class)
    private UUID customerId;

    private String name;

    @Column(name = "current_queue")
    private Boolean currentQueue;

    @JsonIgnore
    @OneToMany(mappedBy = "queue", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AlertQueueEntry> entries;

    public AlertQueue() {}

    public AlertQueue(UUID queueId, UUID customerId) {
        this.queueId = queueId;
        this.customerId = customerId;
    }

    public UUID getQueueId() {
        return queueId;
    }

    public void setQueueId(UUID queueId) {
        this.queueId = queueId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getCurrentQueue() {
        return currentQueue;
    }

    public void setCurrentQueue(Boolean currentQueue) {
        this.currentQueue = currentQueue;
    }

    public Set<AlertQueueEntry> getEntries() {
        return entries;
    }

    public void setEntries(Set<AlertQueueEntry> entries) {
        this.entries = entries;
    }
}
