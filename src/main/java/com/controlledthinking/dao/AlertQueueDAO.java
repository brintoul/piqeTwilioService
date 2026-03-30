package com.controlledthinking.dao;

import com.controlledthinking.db.AlertQueue;
import com.controlledthinking.db.AlertQueueEntry;
import com.controlledthinking.db.Customer;
import com.controlledthinking.dto.AlertQueuePieces;
import java.math.BigDecimal;
import io.dropwizard.hibernate.AbstractDAO;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;

/**
 *
 * @author brintoul
 */
public class AlertQueueDAO extends AbstractDAO<AlertQueueEntry> {

    public AlertQueueDAO(SessionFactory factory) {
        super(factory);
    }

    public Integer findMaxOrder(UUID queueId) {
        String jpql = "SELECT MAX(e.orderWithinQueue) FROM AlertQueueEntry e WHERE e.queue.queueId = :queueId";
        Integer maxOrder = currentSession()
                .createQuery(jpql, Integer.class)
                .setParameter("queueId", queueId)
                .uniqueResult();
        return (maxOrder != null) ? maxOrder : 0;
    }

    public AlertQueueEntry create(AlertQueueEntry entry, UUID queueUuid) {
        AlertQueue theQueue = currentSession().byId(AlertQueue.class).getReference(queueUuid);
        entry.setQueue(theQueue);
        Integer maxOrder = findMaxOrder(queueUuid);
        entry.setOrderWithinQueue(maxOrder + 1);
        return persist(entry);
    }

    public List<AlertQueuePieces> findAlertPieces(final String queueId) {
    return currentSession()
        .createNamedQuery("AlertQueue.findByQueueId", AlertQueuePieces.class)
        .setParameter("queueId", UUID.fromString(queueId))
        .getResultList();
    }

    public void setAlerted(final String queueId, final String phoneNumber) {
        currentSession()
            .createNamedMutationQuery("AlertQueueEntry.setAlerted")
            .setParameter("queueId", UUID.fromString(queueId))
            .setParameter("phoneNumber", phoneNumber)
            .executeUpdate();        
    }
    
    public void setCurrentQueue(String queueId) {
        // Get the customer_id for this queue
        String customerJpql = "SELECT q.customerId FROM AlertQueue q WHERE q.queueId = :queueId";
        UUID customerId = currentSession()
                .createQuery(customerJpql, UUID.class)
                .setParameter("queueId", UUID.fromString(queueId))
                .getSingleResult();

        // Deactivate all queues for this customer
        String deactivateJpql = "UPDATE AlertQueue q SET q.currentQueue = false WHERE q.customerId = :customerId";
        currentSession()
                .createMutationQuery(deactivateJpql)
                .setParameter("customerId", customerId)
                .executeUpdate();

        // Activate the specified queue
        String activateJpql = "UPDATE AlertQueue q SET q.currentQueue = true WHERE q.queueId = :queueId";
        currentSession()
                .createMutationQuery(activateJpql)
                .setParameter("queueId", UUID.fromString(queueId))
                .executeUpdate();
    }

    public List<AlertQueue> findQueuesByCustomer(String customerId) {
        String jpql = "SELECT q FROM AlertQueue q WHERE q.customerId = :customerId";
        return currentSession()
                .createQuery(jpql, AlertQueue.class)
                .setParameter("customerId", UUID.fromString(customerId))
                .getResultList();
    }

    public BigDecimal getCustomerBalance(UUID customerId) {
        Customer customer = currentSession().get(Customer.class, customerId);
        return customer != null ? customer.getCreditBalance() : BigDecimal.ZERO;
    }

    public long countQueuesByCustomer(String customerId) {
        return currentSession()
                .createQuery("SELECT COUNT(q) FROM AlertQueue q WHERE q.customerId = :customerId", Long.class)
                .setParameter("customerId", UUID.fromString(customerId))
                .getSingleResult();
    }

    public AlertQueue createQueue(AlertQueue queue) {
        currentSession().persist(queue);
        return queue;
    }

    public void deleteQueue(String queueId) {
        UUID queueUuid = UUID.fromString(queueId);
        // Delete messages first (FK constraint — not covered by cascade)
        currentSession().createMutationQuery("DELETE FROM AlertMessage m WHERE m.queue.queueId = :queueId")
                .setParameter("queueId", queueUuid)
                .executeUpdate();
        // Delete the queue (CascadeType.ALL removes entries automatically)
        AlertQueue queue = currentSession().get(AlertQueue.class, queueUuid);
        if (queue != null) {
            currentSession().remove(queue);
        }
    }

    public void deleteQueueEntry(String queueId, Integer placement) {
        AlertQueueEntry e = new AlertQueueEntry();
        e.getQueue().setQueueId(UUID.fromString(queueId));
        e.setOrderWithinQueue(placement);
        currentSession().remove(e);
        var updateQuery = currentSession().createMutationQuery("UPDATE AlertQueueEntry e SET e.orderWithinQueue = e.orderWithinQueue - 1 "
                + "WHERE e.orderWithinQueue > :order AND e.queue.queueId = :queueId");
        updateQuery.setParameter("order", placement);
        updateQuery.setParameter("queueId", UUID.fromString(queueId));
        updateQuery.executeUpdate();
    }

}
