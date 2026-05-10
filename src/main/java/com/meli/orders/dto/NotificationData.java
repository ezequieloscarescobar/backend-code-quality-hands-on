package com.meli.orders.dto;

public record NotificationData(
        String to,
        String subject,
        String body,
        String type
) {}
