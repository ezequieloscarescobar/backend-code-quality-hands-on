package com.meli.orders.dto;

public record ItemData(
        String id,
        String name,
        Integer cantidad,
        Double valor
) {}
