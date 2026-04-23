package com.example.hms.notification.channel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.hms.notification.model.NotificationMedium;
import com.example.hms.notification.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "notification.whatsapp", name = "enabled", havingValue = "true")
public class WhatsAppNotificationChannel implements NotificationChannel {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppNotificationChannel.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${notification.whatsapp.provider:generic}")
    private String provider;

    @Value("${notification.whatsapp.api-url}")
    private String apiUrl;

    @Value("${notification.whatsapp.api-token:}")
    private String apiToken;

    @Value("${notification.whatsapp.account-sid:}")
    private String accountSid;

    @Value("${notification.whatsapp.auth-token:}")
    private String authToken;

    @Value("${notification.whatsapp.sender:}")
    private String sender;

    @Value("${notification.whatsapp.default-country-code:+91}")
    private String defaultCountryCode;

    public WhatsAppNotificationChannel(RestTemplate notificationRestTemplate) {
        this.restTemplate = notificationRestTemplate;
    }

    @Override
    public NotificationMedium medium() {
        return NotificationMedium.WHATSAPP;
    }

    @Override
    public void send(NotificationMessage message) {
        String providerKey = provider == null ? "generic" : provider.trim().toLowerCase(Locale.ROOT);
        try {
            if ("twilio".equals(providerKey)) {
                sendTwilio(message);
                return;
            }
            sendGeneric(message);
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException("WhatsApp provider call failed: status=" + ex.getStatusCode().value()
                    + ", body=" + abbreviate(ex.getResponseBodyAsString()), ex);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("WhatsApp provider call failed: " + ex.getMessage(), ex);
        }
    }

    private void sendGeneric(NotificationMessage message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("to", normalizePhoneNumber(message.recipient()));
        payload.put("text", message.body());
        payload.put("message", message.body());
        payload.put("sender", sender);
        payload.put("from", sender);
        payload.put("eventType", message.eventType());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!apiToken.isBlank()) {
            headers.setBearerAuth(apiToken);
        }

        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, new HttpEntity<>(payload, headers), String.class);
        logProviderResponse("generic", message.recipient(), response);
    }

    private void sendTwilio(NotificationMessage message) {
        if (accountSid.isBlank() || authToken.isBlank()) {
            throw new IllegalStateException("Twilio WhatsApp requires notification.whatsapp.account-sid and notification.whatsapp.auth-token");
        }
        if (sender.isBlank()) {
            throw new IllegalStateException("Twilio WhatsApp requires notification.whatsapp.sender");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(accountSid, authToken);

        MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("To", toWhatsAppAddress(message.recipient()));
        payload.add("From", toWhatsAppAddress(sender));
        payload.add("Body", message.body());

        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, new HttpEntity<>(payload, headers), String.class);
        logProviderResponse("twilio", message.recipient(), response);
    }

    private void logProviderResponse(String providerName, String recipient, ResponseEntity<String> response) {
        Map<String, Object> body = parseJsonObject(response.getBody());
        String sid = getString(body, "sid");
        String messageStatus = getString(body, "status");
        String errorCode = getString(body, "error_code");
        String errorMessage = getString(body, "error_message");

        logger.info(
                "event=WHATSAPP_SENT provider={} recipient={} httpStatus={} sid={} providerStatus={} errorCode={} errorMessage={} responseBody={}",
                providerName,
                recipient,
                response.getStatusCode().value(),
                sid == null ? "n/a" : sid,
                messageStatus == null ? "n/a" : messageStatus,
                errorCode == null ? "n/a" : errorCode,
                errorMessage == null ? "n/a" : abbreviate(errorMessage),
                abbreviate(response.getBody())
        );
    }

    private Map<String, Object> parseJsonObject(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(body, new TypeReference<>() { });
        } catch (RuntimeException ex) {
            return Map.of();
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String getString(Map<String, Object> body, String field) {
        Object value = body.get(field);
        return value == null ? null : value.toString();
    }

    private String toWhatsAppAddress(String value) {
        String normalized = normalizePhoneNumber(value);
        return normalized.startsWith("whatsapp:") ? normalized : "whatsapp:" + normalized;
    }

    private String normalizePhoneNumber(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("whatsapp:")) {
            String inner = trimmed.substring("whatsapp:".length());
            return "whatsapp:" + normalizePhoneNumber(inner);
        }

        StringBuilder digits = new StringBuilder();
        boolean hasLeadingPlus = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char current = trimmed.charAt(i);
            if (current == '+' && digits.isEmpty() && !hasLeadingPlus) {
                hasLeadingPlus = true;
            } else if (Character.isDigit(current)) {
                digits.append(current);
            }
        }

        if (digits.isEmpty()) {
            return trimmed;
        }
        if (hasLeadingPlus) {
            return "+" + digits;
        }
        if (digits.length() == 10 && defaultCountryCode != null && !defaultCountryCode.isBlank()) {
            return normalizeCountryCode(defaultCountryCode) + digits;
        }
        if (digits.length() >= 11) {
            return "+" + digits;
        }
        return digits.toString();
    }

    private String normalizeCountryCode(String rawCountryCode) {
        String trimmed = rawCountryCode.trim();
        if (trimmed.startsWith("+")) {
            return trimmed;
        }
        return "+" + trimmed;
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "empty_response";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
