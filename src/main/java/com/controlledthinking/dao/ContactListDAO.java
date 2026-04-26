package com.controlledthinking.dao;

import com.controlledthinking.db.ContactList;
import com.controlledthinking.db.Customer;
import com.controlledthinking.dto.ContactListSummary;
import io.dropwizard.hibernate.AbstractDAO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;

public class ContactListDAO extends AbstractDAO<ContactList> {

    public ContactListDAO(SessionFactory factory) {
        super(factory);
    }

    public ContactList create(ContactList contactList) {
        return persist(contactList);
    }

    public Optional<ContactList> findByIdAndCustomerId(String id, UUID customerId) {
        return currentSession()
            .createQuery(
                "FROM ContactList cl LEFT JOIN FETCH cl.people WHERE cl.id = :id AND cl.customer.customerId = :cid",
                ContactList.class)
            .setParameter("id", id)
            .setParameter("cid", customerId)
            .uniqueResultOptional();
    }

    public List<ContactListSummary> findSummariesByCustomerId(UUID customerId) {
        return currentSession()
            .createQuery(
                "SELECT new com.controlledthinking.dto.ContactListSummary(cl.id, cl.name, cl.uploadedAt, SIZE(cl.people)) " +
                "FROM ContactList cl WHERE cl.customer.customerId = :cid ORDER BY cl.uploadedAt DESC",
                ContactListSummary.class)
            .setParameter("cid", customerId)
            .list();
    }

    public boolean removeContact(String listId, String personId, UUID customerId) {
        Optional<ContactList> listOpt = findByIdAndCustomerId(listId, customerId);
        if (listOpt.isEmpty()) return false;
        ContactList list = listOpt.get();
        boolean removed = list.getPeople().removeIf(p -> p.getId().equals(personId));
        if (removed) persist(list);
        return removed;
    }

    public Customer getCustomerReference(UUID customerId) {
        return currentSession().byId(Customer.class).getReference(customerId);
    }
}
