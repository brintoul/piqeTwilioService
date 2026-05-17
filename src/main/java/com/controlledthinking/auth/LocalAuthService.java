package com.controlledthinking.auth;

import com.controlledthinking.db.AppUser;
import com.controlledthinking.db.Customer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class LocalAuthService implements AuthService {

    private final JwtUtil jwtUtil;
    private final SessionFactory sessionFactory;

    public LocalAuthService(JwtUtil jwtUtil, SessionFactory sessionFactory) {
        this.jwtUtil = jwtUtil;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Optional<User> validateToken(String token) {
        return jwtUtil.parseToken(token);
    }

    @Override
    public User registerUser(String email, String password) {
        throw new UnsupportedOperationException("Registration is not supported in local auth mode");
    }

    @Override
    public String loginUser(String email, String password) {
        throw new UnsupportedOperationException("Token login is not supported in local auth mode");
    }

    @Override
    public User findOrCreateUser(String provider, String subjectId,
                                  String email, String preferredUsername) {
        try (Session session = sessionFactory.openSession()) {
            Optional<AppUser> existing = session
                .createNamedQuery("AppUser.findByOAuthSubject", AppUser.class)
                .setParameter("provider", provider)
                .setParameter("subjectId", subjectId)
                .uniqueResultOptional();

            if (existing.isPresent()) {
                return toUser(existing.get());
            }

            Transaction tx = session.beginTransaction();
            try {
                Customer customer = new Customer();
                customer.setCustomerId(UUID.randomUUID());
                customer.setName(preferredUsername != null ? preferredUsername : email);
                customer.setCreditBalance(new BigDecimal("1.00"));
                session.persist(customer);

                String username = resolveUniqueUsername(session, preferredUsername, provider, subjectId);

                AppUser newUser = new AppUser();
                newUser.setUsername(username);
                newUser.setEmail(email);
                newUser.setOauthProvider(provider);
                newUser.setOauthSubjectId(subjectId);
                newUser.setCustomer(customer);
                session.persist(newUser);

                tx.commit();
                return toUser(newUser);
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        }
    }

    private String resolveUniqueUsername(Session session, String preferred,
                                          String provider, String subjectId) {
        if (preferred != null) {
            Optional<AppUser> clash = session
                .createNamedQuery("AppUser.findByUsername", AppUser.class)
                .setParameter("username", preferred)
                .uniqueResultOptional();
            if (clash.isEmpty()) {
                return preferred;
            }
        }
        return provider + "_" + subjectId.substring(0, Math.min(8, subjectId.length()));
    }

    private User toUser(AppUser appUser) {
        UUID customerId = appUser.getCustomer() != null
            ? appUser.getCustomer().getCustomerId()
            : null;
        return new User(appUser.getUsername(), Set.of("USER"), customerId);
    }
}
