package com.example.hms.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class NotificationHttpConfig {

    @Value("${notification.http.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${notification.http.read-timeout-ms:10000}")
    private int readTimeoutMs;

    @Bean
    public RestTemplate notificationRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }
}
