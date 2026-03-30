package com.controlledthinking.db;

import com.controlledthinking.util.UUIDCharConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "CUSTOMER")
public class Customer {

    @Id
    @Column(name = "customer_id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID customerId;

    private String name;

    @Column(name = "credit_balance", nullable = false)
    private BigDecimal creditBalance = BigDecimal.ZERO;

    public Customer() {}

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(BigDecimal creditBalance) {
        this.creditBalance = creditBalance;
    }
}
