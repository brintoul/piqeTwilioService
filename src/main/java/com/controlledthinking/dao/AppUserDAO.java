package com.controlledthinking.dao;

import com.controlledthinking.db.AppUser;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.Optional;

public class AppUserDAO extends AbstractDAO<AppUser> {

    public AppUserDAO(SessionFactory factory) {
        super(factory);
    }

    public Optional<AppUser> findByUsername(String username) {
        return currentSession()
            .createNamedQuery("AppUser.findByUsername", AppUser.class)
            .setParameter("username", username)
            .uniqueResultOptional();
    }

    public Optional<AppUser> findByOAuthSubject(String provider, String subjectId) {
        return currentSession()
            .createNamedQuery("AppUser.findByOAuthSubject", AppUser.class)
            .setParameter("provider", provider)
            .setParameter("subjectId", subjectId)
            .uniqueResultOptional();
    }

    public AppUser create(AppUser user) {
        return persist(user);
    }
}
