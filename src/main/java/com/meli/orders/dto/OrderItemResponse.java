package com.meli.orders.dto;

public record OrderItemResponse(
        String productId,
        String productName,
        Integer quantity,
        Double unitPrice,
        Double total
) {}
