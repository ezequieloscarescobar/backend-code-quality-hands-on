package com.meli.orders.dto;

import java.time.LocalDateTime;

public record ReportData(
        Long id,
        String estado,
        String cliente,
        Double total,
        Double impuestos,
        LocalDateTime fecha,
        Integer cantidadItems
) {}
