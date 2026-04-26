package com.controlledthinking.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "CONTACT_LIST")
public class ContactList {

    @Id
    @UuidGenerator
    @Column(name = "ID", nullable = false, length = 36)
    private String id;

    @Column(name = "NAME", nullable = false, length = 255)
    private String name;

    @Column(name = "UPLOADED_AT", nullable = false)
    private LocalDateTime uploadedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    private Customer customer;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "CONTACT_LIST_PERSON",
        joinColumns = @JoinColumn(name = "CONTACT_LIST_ID"),
        inverseJoinColumns = @JoinColumn(name = "PERSON_OR_ENTITY_ID")
    )
    private List<PersonOrEntity> people = new ArrayList<>();

    public ContactList() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public List<PersonOrEntity> getPeople() { return people; }
    public void setPeople(List<PersonOrEntity> people) { this.people = people; }
}
