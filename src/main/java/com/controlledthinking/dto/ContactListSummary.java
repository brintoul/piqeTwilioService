package com.controlledthinking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class ContactListSummary {

    private final String id;
    private final String name;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime uploadedAt;
    private final int personCount;

    public ContactListSummary(String id, String name, LocalDateTime uploadedAt, int personCount) {
        this.id = id;
        this.name = name;
        this.uploadedAt = uploadedAt;
        this.personCount = personCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public int getPersonCount() { return personCount; }
}
