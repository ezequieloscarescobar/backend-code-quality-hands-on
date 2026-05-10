package com.meli.orders.dto;

public record PaymentRequest(
        Long order_id,
        Double monto,
        String metodo,
        String customerMail
) {}
