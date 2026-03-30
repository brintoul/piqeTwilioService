package com.controlledthinking.client;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.net.URI;
import java.util.List;

/**
 *
 * @author brintoul
 */
public class TwilioServicesProvider {

    private final String messagingServiceSid;

    public TwilioServicesProvider(String accountSid, String authToken, String messagingServiceSid) {
        Twilio.init(accountSid, authToken);
        this.messagingServiceSid = messagingServiceSid;
    }

    public Message sendSms(String from, String to, String body) {
        return Message.creator(
                new PhoneNumber(to),
                messagingServiceSid,
                body
        ).setStatusCallback("https://www.controlledthinking.com/api/sms/status").create();
    }

    public Message sendMms(String from, String to, String body, String mediaUrl) {
        return Message.creator(
                new PhoneNumber(to),
                messagingServiceSid,
                body
        ).setMediaUrl(List.of(URI.create(mediaUrl)))
         .setStatusCallback("https://www.controlledthinking.com/api/sms/status").create();
    }
}
