package com.meli.orders.dto;

import java.util.List;

public record OrderRequest(
        String customer,
        String mail,
        String nombre,
        List<ItemData> productos,
        String cupon
) {}
