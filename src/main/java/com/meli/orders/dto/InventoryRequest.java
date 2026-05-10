package com.meli.orders.dto;

public record InventoryRequest(
        String productId,
        Integer units,
        String operationType
) {}
