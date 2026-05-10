package com.meli.orders.client;

import com.meli.orders.dto.NotificationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NotificationClient {

    private final RestTemplate restTemplate;

    @Value("${notifications.service.url}")
    private String notificationsUrl;

    public NotificationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void send(NotificationData data) {
        try {
            restTemplate.postForObject(notificationsUrl + "/api/notifications", data, Void.class);
        } catch (Exception ignored) {
        }
    }
}
