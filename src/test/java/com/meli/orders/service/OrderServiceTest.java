package com.meli.orders.service;

import com.meli.orders.client.InventoryClient;
import com.meli.orders.client.NotificationClient;
import com.meli.orders.client.PaymentClient;
import com.meli.orders.dto.*;
import com.meli.orders.mapper.OrderMapper;
import com.meli.orders.model.Order;
import com.meli.orders.model.OrderItem;
import com.meli.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private NotificationClient notificationClient;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order();
        sampleOrder.setId(1L);
        sampleOrder.setCustomerId("customer-1");
        sampleOrder.setCustomerEmail("test@mail.com");
        sampleOrder.setStatus("PENDING");
        sampleOrder.setTotal(121.0);
        sampleOrder.setSubtotal(100.0);
        sampleOrder.setTaxes(21.0);
        sampleOrder.setDiscount(0.0);
        sampleOrder.setCreatedAt(LocalDateTime.now());
        sampleOrder.setUpdatedAt(LocalDateTime.now());
        sampleOrder.setNotified(false);
        sampleOrder.setAttempts(0);

        OrderItem item = new OrderItem();
        item.setProductId("prod-1");
        item.setProductName("Product 1");
        item.setQty(2);
        item.setPrice(50.0);
        item.setTotalPrice(100.0);
        item.setOrder(sampleOrder);
        sampleOrder.setItems(List.of(item));
    }

    @Test
    void createOrder_withValidRequest_returnsOrderResponse() {
        OrderRequest request = new OrderRequest(
                "customer-1", "test@mail.com", "Test User",
                List.of(new ItemData("prod-1", "Product 1", 2, 50.0)),
                null
        );

        when(inventoryClient.reserveStock(any(), any())).thenReturn(true);
        when(orderRepository.save(any())).thenReturn(sampleOrder);
        when(orderMapper.toResponse(any())).thenReturn(buildSampleResponse());

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        verify(orderRepository, times(2)).save(any());
        verify(inventoryClient, times(1)).reserveStock("prod-1", 2);
        verify(notificationClient, times(1)).send(any());
    }

    @Test
    void createOrder_withoutCustomer_throwsException() {
        OrderRequest request = new OrderRequest(null, "test@mail.com", "Test", List.of(), null);
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrder_withCouponDESCUENTO10_appliesDiscount() {
        OrderRequest request = new OrderRequest(
                "customer-1", "test@mail.com", "Test User",
                List.of(new ItemData("prod-1", "Product 1", 1, 100.0)),
                "DESCUENTO10"
        );

        when(inventoryClient.reserveStock(any(), any())).thenReturn(true);
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(orderMapper.toResponse(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return new OrderResponse(o.getId(), o.getStatus(), o.getCustomerId(),
                    o.getCustomerEmail(), o.getTotal(), o.getSubtotal(),
                    o.getTaxes(), o.getDiscount(), List.of(), o.getCreatedAt(), null);
        });

        OrderResponse response = orderService.createOrder(request);

        assertEquals(107.69, response.totalAmount(), 0.01);
    }

    @Test
    void confirmOrder_whenPending_changesStatusToConfirmed() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenReturn(sampleOrder);
        when(orderMapper.toResponse(any())).thenReturn(buildSampleResponse());

        orderService.confirmOrder(1L);

        assertEquals("CONFIRMED", sampleOrder.getStatus());
        verify(inventoryClient, times(1)).reserveStock(any(), any());
    }

    @Test
    void confirmOrder_whenNotPending_throwsException() {
        sampleOrder.setStatus("PAID");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        assertThrows(IllegalStateException.class, () -> orderService.confirmOrder(1L));
    }

    @Test
    void payOrder_withSuccessfulPayment_changesStatusToPaid() {
        sampleOrder.setStatus("CONFIRMED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenReturn(sampleOrder);
        when(paymentClient.processPayment(any())).thenReturn(new PaymentResponse("pay-123", "SUCCESS", "ok"));
        when(orderMapper.toResponse(any())).thenReturn(buildSampleResponse());

        orderService.payOrder(1L, "CREDIT_CARD");

        assertEquals("PAID", sampleOrder.getStatus());
        assertEquals("pay-123", sampleOrder.getPaymentId());
        verify(inventoryClient, times(1)).confirmStock(any(), any());
    }

    @Test
    void payOrder_whenAlreadyPaid_throwsException() {
        sampleOrder.setStatus("PAID");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        assertThrows(IllegalStateException.class, () -> orderService.payOrder(1L, "CREDIT_CARD"));
    }

    @Test
    void cancelOrder_whenPaid_processesRefund() {
        sampleOrder.setStatus("PAID");
        sampleOrder.setPaymentId("pay-123");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(paymentClient.refundPayment("pay-123")).thenReturn(new PaymentResponse("pay-123", "REFUNDED", "ok"));
        when(orderRepository.save(any())).thenReturn(sampleOrder);
        when(orderMapper.toResponse(any())).thenReturn(buildSampleResponse());

        orderService.cancelOrder(1L);

        assertEquals("CANCELLED", sampleOrder.getStatus());
        verify(paymentClient, times(1)).refundPayment("pay-123");
        verify(inventoryClient, times(2)).releaseStock(any(), any());
    }

    @Test
    void getTotalRevenue_sumsPaidOrders() {
        Order paid1 = new Order(); paid1.setTotal(100.0); paid1.setStatus("PAID"); paid1.setItems(new ArrayList<>());
        Order paid2 = new Order(); paid2.setTotal(200.0); paid2.setStatus("PAID"); paid2.setItems(new ArrayList<>());
        when(orderRepository.findByStatus("PAID")).thenReturn(List.of(paid1, paid2));

        double revenue = orderService.getTotalRevenue();

        assertEquals(300.0, revenue);
    }

    private OrderResponse buildSampleResponse() {
        return new OrderResponse(1L, "PENDING", "customer-1", "test@mail.com",
                121.0, 100.0, 21.0, 0.0, List.of(), LocalDateTime.now(), null);
    }
}
