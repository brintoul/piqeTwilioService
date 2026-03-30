package com.controlledthinking.dao;

import com.controlledthinking.db.Customer;
import com.controlledthinking.db.PersonOrEntity;
import io.dropwizard.hibernate.AbstractDAO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;

/**
 *
 * @author brintoul
 */
public class PersonOrEntityDAO extends AbstractDAO<PersonOrEntity> {

    public PersonOrEntityDAO(SessionFactory factory) {
        super(factory);
    }

    public Optional<PersonOrEntity> findById(String id) {
        return Optional.ofNullable(get(id));
    }

    public PersonOrEntity create(PersonOrEntity person) {
        return persist(person);
    }

    public List<PersonOrEntity> queryByString(String queryString) {
        return super.query(queryString).list();
    }

    public Customer getCustomerReference(UUID customerId) {
        return currentSession().byId(Customer.class).getReference(customerId);
    }

    public boolean existsByNameAndPhone(String firstName, String lastName, String phoneNumber) {
        Long count = currentSession()
                .createQuery("SELECT COUNT(p) FROM PersonOrEntity p WHERE p.firstName = :fn AND p.lastName = :ln AND p.phoneNumber = :pn", Long.class)
                .setParameter("fn", firstName)
                .setParameter("ln", lastName)
                .setParameter("pn", phoneNumber)
                .uniqueResult();
        return count != null && count > 0;
    }
}
