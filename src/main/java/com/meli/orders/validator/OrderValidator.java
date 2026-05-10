package com.meli.orders.validator;

import com.meli.orders.dto.OrderRequest;
import org.springframework.stereotype.Component;

@Component
public class OrderValidator {

    public void validate(OrderRequest request) {
        if (request.customer() == null || request.customer().isBlank()) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (request.productos() == null || request.productos().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        if (request.mail() == null || request.mail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
    }

    public boolean isValidForPayment(com.meli.orders.model.Order order) {
        return order.getTotal() != null && order.getTotal() > 0;
    }
}
