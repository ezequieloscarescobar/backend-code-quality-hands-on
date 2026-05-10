package com.meli.orders.controller;

import com.meli.orders.dto.OrderRequest;
import com.meli.orders.dto.OrderResponse;
import com.meli.orders.service.OrderService;
import com.meli.orders.validator.OrderValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderValidator orderValidator;

    public OrderController(OrderService orderService, OrderValidator orderValidator) {
        this.orderService = orderService;
        this.orderValidator = orderValidator;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        if (request.customer() == null || request.customer().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Customer required"));
        }
        if (request.mail() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email required"));
        }
        if (request.productos() == null || request.productos().isEmpty()) {
            return ResponseEntity.status(422).body(Map.of("error", "No products"));
        }
        try {
            OrderResponse response = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal error"));
        }
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmOrder(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(orderService.confirmOrder(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> payOrder(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String method = body.get("method");
        if (method == null || method.isBlank()) {
            return ResponseEntity.badRequest().body("Payment method required");
        }
        try {
            OrderResponse response = orderService.payOrder(id, method);
            if (response.orderStatus().equals("PAID")) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(402).body(response);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(orderService.cancelOrder(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(orderService.getOrder(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "Order not found"));
        }
    }

    @GetMapping
    public List<OrderResponse> listOrders(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String customerId) {
        if (status != null) {
            return orderService.getOrdersByStatus(status);
        }
        if (customerId != null) {
            return orderService.getByCustomer(customerId);
        }
        return orderService.getAllOrders();
    }

    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Double>> getRevenue() {
        return ResponseEntity.ok(Map.of("totalRevenue", orderService.getTotalRevenue()));
    }
}
