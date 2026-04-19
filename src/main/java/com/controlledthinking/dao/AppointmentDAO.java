package com.controlledthinking.dao;

import com.controlledthinking.db.Appointment;
import com.controlledthinking.db.Customer;
import io.dropwizard.hibernate.AbstractDAO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;

public class AppointmentDAO extends AbstractDAO<Appointment> {

    public AppointmentDAO(SessionFactory factory) {
        super(factory);
    }

    public Appointment create(Appointment appointment) {
        return persist(appointment);
    }

    public void delete(Appointment appointment) {
        currentSession().remove(appointment);
    }

    public Optional<Appointment> findById(String id) {
        return Optional.ofNullable(get(id));
    }

    public List<Appointment> findByCustomerAndDateRange(UUID customerId, LocalDateTime from, LocalDateTime to) {
        return currentSession()
                .createQuery("FROM Appointment a WHERE a.customer.customerId = :cid AND a.appointmentTime >= :from AND a.appointmentTime <= :to ORDER BY a.appointmentTime", Appointment.class)
                .setParameter("cid", customerId)
                .setParameter("from", from)
                .setParameter("to", to)
                .list();
    }

    public Customer getCustomerReference(UUID customerId) {
        return currentSession().byId(Customer.class).getReference(customerId);
    }
}
