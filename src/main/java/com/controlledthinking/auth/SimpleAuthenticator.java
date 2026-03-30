package com.controlledthinking.auth;

import com.controlledthinking.db.AppUser;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.Optional;
import java.util.Set;

public class SimpleAuthenticator implements Authenticator<BasicCredentials, User> {

    private final SessionFactory sessionFactory;

    public SimpleAuthenticator(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Optional<User> authenticate(BasicCredentials credentials) throws AuthenticationException {
        try (Session session = sessionFactory.openSession()) {
            Optional<AppUser> appUser = session
                .createNamedQuery("AppUser.findByUsername", AppUser.class)
                .setParameter("username", credentials.getUsername())
                .uniqueResultOptional();

            if (appUser.isPresent() && appUser.get().getPassword().equals(credentials.getPassword())) {
                AppUser user = appUser.get();
                return Optional.of(new User(
                    user.getUsername(),
                    Set.of("ADMIN"),
                    user.getCustomer().getCustomerId()
                ));
            }
        }
        return Optional.empty();
    }
}
