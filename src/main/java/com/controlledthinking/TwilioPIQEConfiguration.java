package com.controlledthinking;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;

public class TwilioPIQEConfiguration extends Configuration {

    @NotEmpty
    private String twilioAccountSid;

    @NotEmpty
    private String twilioAuthToken;

    @NotEmpty
    private String twilioMessagingServiceSid;

    @NotNull
    private BigDecimal costPerMessage = new BigDecimal("0.03");

    @NotNull
    private BigDecimal costPerMms = new BigDecimal("0.10");

    private BigDecimal lowCreditThreshold = new BigDecimal("1.00");

    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @NotEmpty
    private String jwtSecret;

    @NotEmpty
    private String frontendBaseUrl;

    @NotEmpty
    private String mediaStoragePath;

    @NotEmpty
    private String mediaBaseUrl;

    private OAuthProviderConfig google    = new OAuthProviderConfig();
    private OAuthProviderConfig microsoft = new OAuthProviderConfig();
    private OAuthProviderConfig github    = new OAuthProviderConfig();

    // Existing getters/setters
    public String getTwilioAccountSid() { return twilioAccountSid; }
    public void setTwilioAccountSid(String sid) { this.twilioAccountSid = sid; }

    public String getTwilioAuthToken() { return twilioAuthToken; }
    public void setTwilioAuthToken(String token) { this.twilioAuthToken = token; }

    public String getTwilioMessagingServiceSid() { return twilioMessagingServiceSid; }
    public void setTwilioMessagingServiceSid(String sid) { this.twilioMessagingServiceSid = sid; }

    public BigDecimal getCostPerMessage() { return costPerMessage; }
    public void setCostPerMessage(BigDecimal costPerMessage) { this.costPerMessage = costPerMessage; }

    public BigDecimal getCostPerMms() { return costPerMms; }
    public void setCostPerMms(BigDecimal costPerMms) { this.costPerMms = costPerMms; }

    public BigDecimal getLowCreditThreshold() { return lowCreditThreshold; }
    public void setLowCreditThreshold(BigDecimal lowCreditThreshold) { this.lowCreditThreshold = lowCreditThreshold; }

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory factory) {
        this.database = factory;
    }

    // JWT
    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

    // Frontend URL
    public String getFrontendBaseUrl() { return frontendBaseUrl; }
    public void setFrontendBaseUrl(String frontendBaseUrl) { this.frontendBaseUrl = frontendBaseUrl; }

    // Media
    public String getMediaStoragePath() { return mediaStoragePath; }
    public void setMediaStoragePath(String mediaStoragePath) { this.mediaStoragePath = mediaStoragePath; }

    public String getMediaBaseUrl() { return mediaBaseUrl; }
    public void setMediaBaseUrl(String mediaBaseUrl) { this.mediaBaseUrl = mediaBaseUrl; }

    // OAuth providers
    @JsonProperty("google")
    public OAuthProviderConfig getGoogle() { return google; }
    @JsonProperty("google")
    public void setGoogle(OAuthProviderConfig google) { this.google = google; }

    @JsonProperty("microsoft")
    public OAuthProviderConfig getMicrosoft() { return microsoft; }
    @JsonProperty("microsoft")
    public void setMicrosoft(OAuthProviderConfig microsoft) { this.microsoft = microsoft; }

    @JsonProperty("github")
    public OAuthProviderConfig getGithub() { return github; }
    @JsonProperty("github")
    public void setGithub(OAuthProviderConfig github) { this.github = github; }

    public static class OAuthProviderConfig {
        private String clientId     = "";
        private String clientSecret = "";
        private String callbackUrl  = "";

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public String getCallbackUrl() { return callbackUrl; }
        public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

        public boolean isConfigured() {
            return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty();
        }
    }
}
