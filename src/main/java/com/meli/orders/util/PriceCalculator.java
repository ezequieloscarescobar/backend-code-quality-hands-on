package com.meli.orders.util;

import org.springframework.stereotype.Component;

@Component
public class PriceCalculator {

    public double applyDiscount(double subtotal, String coupon) {
        if (coupon == null || coupon.isBlank()) return subtotal;
        double discount = switch (coupon) {
            case "DESCUENTO10" -> subtotal * 0.10;
            case "DESCUENTO20" -> subtotal * 0.20;
            case "VERANO50" -> subtotal * 0.50;
            default -> 0.0;
        };
        return subtotal - discount;
    }

    public double calculateTaxes(double amount) {
        return amount * 0.21;
    }

    public double calculateTotal(double subtotal, String coupon) {
        double afterDiscount = applyDiscount(subtotal, coupon);
        return afterDiscount + calculateTaxes(afterDiscount);
    }
}
