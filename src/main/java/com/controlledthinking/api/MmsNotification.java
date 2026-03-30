package com.controlledthinking.api;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class MmsNotification {

    @NotEmpty
    private List<String> personOrEntityIds;

    private String messageBody;

    @NotEmpty
    private String mediaId;

    public List<String> getPersonOrEntityIds() {
        return personOrEntityIds;
    }

    public void setPersonOrEntityIds(List<String> personOrEntityIds) {
        this.personOrEntityIds = personOrEntityIds;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }
}
