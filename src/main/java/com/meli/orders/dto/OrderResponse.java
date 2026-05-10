package com.meli.orders.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String orderStatus,
        String clientId,
        String clientEmail,
        Double totalAmount,
        Double subtotalAmount,
        Double taxAmount,
        Double discountAmount,
        List<OrderItemResponse> orderItems,
        LocalDateTime creationDate,
        String paymentReference
) {}
