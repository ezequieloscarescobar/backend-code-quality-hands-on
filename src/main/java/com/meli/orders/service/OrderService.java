package com.meli.orders.service;

import com.meli.orders.client.InventoryClient;
import com.meli.orders.client.NotificationClient;
import com.meli.orders.client.PaymentClient;
import com.meli.orders.dto.*;
import com.meli.orders.mapper.OrderMapper;
import com.meli.orders.model.Order;
import com.meli.orders.model.OrderItem;
import com.meli.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;
    private final NotificationClient notificationClient;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        PaymentClient paymentClient,
                        InventoryClient inventoryClient,
                        NotificationClient notificationClient,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
        this.inventoryClient = inventoryClient;
        this.notificationClient = notificationClient;
        this.orderMapper = orderMapper;
    }

    public OrderResponse createOrder(OrderRequest request) {
        if (request.customer() == null || request.customer().isBlank()) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (request.productos() == null || request.productos().isEmpty()) {
            throw new IllegalArgumentException("Products required");
        }

        Order order = new Order();
        order.setCustomerId(request.customer());
        order.setCustomerEmail(request.mail());
        order.setCustomerName(request.nombre());
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setNotified(false);
        order.setAttempts(0);
        order.setCouponCode(request.cupon());

        List<OrderItem> items = new ArrayList<>();
        double subtotal = 0.0;

        for (ItemData itemData : request.productos()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemData.id());
            item.setProductName(itemData.name());
            item.setQty(itemData.cantidad());
            item.setPrice(itemData.valor());
            item.setTotalPrice(itemData.valor() * itemData.cantidad());
            item.setOrder(order);
            items.add(item);
            subtotal += itemData.valor() * itemData.cantidad();
        }

        order.setItems(items);
        order.setSubtotal(subtotal);

        double discount = 0.0;
        if (request.cupon() != null) {
            if (request.cupon().equals("DESCUENTO10")) {
                discount = subtotal * 0.10;
            } else if (request.cupon().equals("DESCUENTO20")) {
                discount = subtotal * 0.20;
            } else if (request.cupon().equals("VERANO50")) {
                discount = subtotal * 0.50;
            }
        }

        double taxes = (subtotal - discount) * 0.21;
        double total = subtotal - discount + taxes;

        order.setDiscount(discount);
        order.setTaxes(taxes);
        order.setTotal(total);

        for (OrderItem item : items) {
            inventoryClient.reserveStock(item.getProductId(), item.getQty());
        }

        Order saved = orderRepository.save(order);

        notificationClient.send(new NotificationData(
                order.getCustomerEmail(),
                "Order created",
                "Your order #" + saved.getId() + " has been created. Total: " + total,
                "EMAIL"
        ));
        saved.setNotified(true);
        orderRepository.save(saved);

        return orderMapper.toResponse(saved);
    }

    public OrderResponse confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getStatus().equals("PENDING")) {
            throw new IllegalStateException("Order cannot be confirmed");
        }

        order.setStatus("CONFIRMED");
        order.setUpdatedAt(LocalDateTime.now());

        for (OrderItem item : order.getItems()) {
            inventoryClient.reserveStock(item.getProductId(), item.getQty());
        }

        Order saved = orderRepository.save(order);

        notificationClient.send(new NotificationData(
                order.getCustomerEmail(),
                "Order confirmed",
                "Your order #" + orderId + " has been confirmed.",
                "EMAIL"
        ));

        return orderMapper.toResponse(saved);
    }

    public OrderResponse payOrder(Long orderId, String paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus().equals("PAID")) {
            throw new IllegalStateException("Order is already paid");
        }

        if (!order.getStatus().equals("CONFIRMED") && !order.getStatus().equals("PENDING")) {
            throw new IllegalStateException("Order cannot be paid");
        }

        order.setAttempts(order.getAttempts() + 1);
        orderRepository.save(order);

        PaymentRequest paymentRequest = new PaymentRequest(
                orderId,
                order.getTotal(),
                paymentMethod,
                order.getCustomerEmail()
        );

        PaymentResponse paymentResponse = paymentClient.processPayment(paymentRequest);

        if (paymentResponse.status().equals("SUCCESS") || paymentResponse.status().equals("APPROVED")) {
            order.setStatus("PAID");
            order.setPaymentId(paymentResponse.paymentId());
            order.setPaidAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            for (OrderItem item : order.getItems()) {
                inventoryClient.confirmStock(item.getProductId(), item.getQty());
            }

            notificationClient.send(new NotificationData(
                    order.getCustomerEmail(),
                    "Payment successful",
                    "Your payment for order #" + orderId + " was successful. Payment ID: " + paymentResponse.paymentId(),
                    "EMAIL"
            ));
            order.setNotified(true);
        } else {
            notificationClient.send(new NotificationData(
                    order.getCustomerEmail(),
                    "Payment failed",
                    "Your payment for order #" + orderId + " failed. Please try again.",
                    "EMAIL"
            ));
        }

        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus().equals("CANCELLED")) {
            throw new IllegalStateException("Order is already cancelled");
        }

        if (order.getStatus().equals("PAID")) {
            PaymentResponse refund = paymentClient.refundPayment(order.getPaymentId());
            if (!refund.status().equals("SUCCESS") && !refund.status().equals("REFUNDED")) {
                throw new IllegalStateException("Refund failed, cannot cancel order");
            }
            for (OrderItem item : order.getItems()) {
                inventoryClient.releaseStock(item.getProductId(), item.getQty());
            }
        }

        if (!order.getStatus().equals("PAID")) {
            for (OrderItem item : order.getItems()) {
                inventoryClient.releaseStock(item.getProductId(), item.getQty());
            }
        }

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());

        notificationClient.send(new NotificationData(
                order.getCustomerEmail(),
                "Order cancelled",
                "Your order #" + orderId + " has been cancelled.",
                "EMAIL"
        ));
        order.setNotified(false);

        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return orderMapper.toResponse(order);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status).stream()
                .map(o -> {
                    List<OrderItemResponse> items = o.getItems().stream()
                            .map(i -> new OrderItemResponse(
                                    i.getProductId(),
                                    i.getProductName(),
                                    i.getQty(),
                                    i.getPrice(),
                                    i.getPrice() * i.getQty()
                            ))
                            .toList();
                    return new OrderResponse(
                            o.getId(),
                            o.getStatus(),
                            o.getCustomerId(),
                            o.getCustomerEmail(),
                            o.getTotal(),
                            o.getSubtotal(),
                            o.getTaxes(),
                            o.getDiscount(),
                            items,
                            o.getCreatedAt(),
                            o.getPaymentId()
                    );
                })
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getByCustomer(String customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        List<OrderResponse> result = new ArrayList<>();
        for (Order o : orders) {
            result.add(orderMapper.toResponse(o));
        }
        return result;
    }

    public double getTotalRevenue() {
        List<Order> paid = orderRepository.findByStatus("PAID");
        double total = 0;
        for (Order o : paid) {
            total += o.getTotal();
        }
        return total;
    }

    public double calculateDiscount(String coupon, double subtotal) {
        if (coupon == null) return 0.0;
        if (coupon.equals("DESCUENTO10")) return subtotal * 0.10;
        if (coupon.equals("DESCUENTO20")) return subtotal * 0.20;
        if (coupon.equals("VERANO50")) return subtotal * 0.50;
        return 0.0;
    }
}
