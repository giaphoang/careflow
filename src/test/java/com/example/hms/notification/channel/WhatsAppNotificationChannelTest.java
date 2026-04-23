package com.example.hms.notification.channel;

import com.example.hms.notification.model.NotificationMedium;
import com.example.hms.notification.model.NotificationMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WhatsAppNotificationChannelTest {

    @Test
    void sendsGenericPayloadWithNormalizedNumber() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WhatsAppNotificationChannel channel = new WhatsAppNotificationChannel(restTemplate);

        ReflectionTestUtils.setField(channel, "provider", "generic");
        ReflectionTestUtils.setField(channel, "apiUrl", "https://provider.example/send");
        ReflectionTestUtils.setField(channel, "apiToken", "token-123");
        ReflectionTestUtils.setField(channel, "sender", "HMS");
        ReflectionTestUtils.setField(channel, "defaultCountryCode", "+91");

        server.expect(requestTo("https://provider.example/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-123"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "to": "+919876543210",
                          "text": "Appointment booked",
                          "message": "Appointment booked",
                          "sender": "HMS",
                          "from": "HMS",
                          "eventType": "APPOINTMENT_BOOKED_PATIENT_WHATSAPP"
                        }
                        """, true))
                .andRespond(withSuccess());

        channel.send(new NotificationMessage(
                NotificationMedium.WHATSAPP,
                "98765 43210",
                "HMS Notification",
                "Appointment booked",
                "APPOINTMENT_BOOKED_PATIENT_WHATSAPP"
        ));

        server.verify();
    }

    @Test
    void sendsTwilioFormPayloadWithWhatsAppPrefix() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WhatsAppNotificationChannel channel = new WhatsAppNotificationChannel(restTemplate);

        ReflectionTestUtils.setField(channel, "provider", "twilio");
        ReflectionTestUtils.setField(channel, "apiUrl", "https://api.twilio.com/2010-04-01/Accounts/AC123/Messages.json");
        ReflectionTestUtils.setField(channel, "accountSid", "AC123");
        ReflectionTestUtils.setField(channel, "authToken", "secret");
        ReflectionTestUtils.setField(channel, "sender", "+14155238886");
        ReflectionTestUtils.setField(channel, "defaultCountryCode", "+91");

        server.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC123/Messages.json"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic QUMxMjM6c2VjcmV0"))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
                .andExpect(content().string("To=whatsapp%3A%2B919876543210&From=whatsapp%3A%2B14155238886&Body=Appointment+booked"))
                .andRespond(withSuccess());

        channel.send(new NotificationMessage(
                NotificationMedium.WHATSAPP,
                "9876543210",
                "HMS Notification",
                "Appointment booked",
                "APPOINTMENT_BOOKED_PATIENT_WHATSAPP"
        ));

        server.verify();
    }
}
