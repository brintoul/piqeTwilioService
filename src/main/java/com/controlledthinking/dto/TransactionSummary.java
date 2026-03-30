package com.controlledthinking.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionSummary {

    private String transactionId;
    private String createdAt;

    public TransactionSummary(String transactionId, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.createdAt = createdAt != null
            ? createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            : null;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
