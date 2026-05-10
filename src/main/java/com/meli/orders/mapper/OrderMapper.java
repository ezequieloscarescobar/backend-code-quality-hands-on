package com.meli.orders.mapper;

import com.meli.orders.dto.OrderItemResponse;
import com.meli.orders.dto.OrderResponse;
import com.meli.orders.dto.ReportData;
import com.meli.orders.model.Order;
import com.meli.orders.model.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getProductId(),
                        i.getProductName(),
                        i.getQty(),
                        i.getPrice(),
                        i.getTotalPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getCustomerId(),
                order.getCustomerEmail(),
                order.getTotal(),
                order.getSubtotal(),
                order.getTaxes(),
                order.getDiscount(),
                items,
                order.getCreatedAt(),
                order.getPaymentId()
        );
    }

    public ReportData toReportData(Order order) {
        return new ReportData(
                order.getId(),
                order.getStatus(),
                order.getCustomerId(),
                order.getTotal(),
                order.getTaxes(),
                order.getCreatedAt(),
                order.getItems().size()
        );
    }
}
