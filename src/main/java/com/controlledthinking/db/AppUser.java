package com.controlledthinking.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "APP_USER")
@NamedQueries({
    @NamedQuery(name = "AppUser.findByUsername",
        query = "SELECT u FROM AppUser u WHERE u.username = :username"),
    @NamedQuery(name = "AppUser.findByOAuthSubject",
        query = "SELECT u FROM AppUser u WHERE u.oauthProvider = :provider AND u.oauthSubjectId = :subjectId")
})
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password", nullable = true, length = 255)
    private String password;

    @Column(name = "email", nullable = true, length = 255)
    private String email;

    @Column(name = "oauth_provider", nullable = true, length = 50)
    private String oauthProvider;

    @Column(name = "oauth_subject_id", nullable = true, length = 255)
    private String oauthSubjectId;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    public AppUser() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }

    public String getOauthSubjectId() { return oauthSubjectId; }
    public void setOauthSubjectId(String oauthSubjectId) { this.oauthSubjectId = oauthSubjectId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
}
