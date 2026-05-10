package com.meli.orders.dto;

public record PaymentResponse(
        String paymentId,
        String status,
        String message
) {}
