package com.controlledthinking.dao;

import com.controlledthinking.db.AlertMessage;
import com.controlledthinking.db.AlertQueue;
import io.dropwizard.hibernate.AbstractDAO;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;

/**
 *
 * @author brintoul
 */
public class AlertQueueMessageDAO extends AbstractDAO<AlertMessage> {

    public AlertQueueMessageDAO(SessionFactory factory) {
        super(factory);
    }

    public boolean createMessage(AlertMessage message, UUID queueUuid) {
        AlertQueue theQueue = currentSession().byId(AlertQueue.class).getReference(queueUuid);
        message.setQueue(theQueue);
        String countJpql = "SELECT COUNT(qm) FROM AlertMessage qm WHERE qm.queue.queueId = :queueId";
        Long count = currentSession()
                .createQuery(countJpql, Long.class)
                .setParameter("queueId", queueUuid)
                .getSingleResult();
        if (count == 0) {
            message.setActive(true);
        }
        persist(message);
        return true;
    }

    public AlertMessage fetchQueueMessage(String queueUuid) {
        String jpql = "SELECT qm FROM AlertMessage qm WHERE qm.queue.queueId = :queueId and qm.active = true";
        AlertMessage theMessage = currentSession()
                .createQuery(jpql, AlertMessage.class)
                .setParameter("queueId", UUID.fromString(queueUuid))
                .getSingleResult();
        return theMessage;
    }

    public List<AlertMessage> fetchAllQueueMessages(String queueUuid) {
        String jpql = "SELECT qm FROM AlertMessage qm WHERE qm.queue.queueId = :queueId";
        return currentSession()
                .createQuery(jpql, AlertMessage.class)
                .setParameter("queueId", UUID.fromString(queueUuid))
                .getResultList();
    }

    public void deleteMessage(Integer messageId) {
        AlertMessage message = currentSession().get(AlertMessage.class, messageId);
        if (message != null) {
            currentSession().remove(message);
        }
    }

    public void setActiveMessage(String queueUuid, Integer messageId) {
        // Deactivate all messages for this queue
        String deactivateJpql = "UPDATE AlertMessage qm SET qm.active = false WHERE qm.queue.queueId = :queueId";
        currentSession()
                .createMutationQuery(deactivateJpql)
                .setParameter("queueId", UUID.fromString(queueUuid))
                .executeUpdate();

        // Activate the specified message
        String activateJpql = "UPDATE AlertMessage qm SET qm.active = true WHERE qm.messageId = :messageId";
        currentSession()
                .createMutationQuery(activateJpql)
                .setParameter("messageId", messageId)
                .executeUpdate();
    }
}
