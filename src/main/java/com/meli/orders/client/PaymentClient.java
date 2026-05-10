package com.meli.orders.client;

import com.meli.orders.dto.PaymentRequest;
import com.meli.orders.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentClient {

    private final RestTemplate restTemplate;

    @Value("${payments.service.url}")
    private String paymentsUrl;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            return restTemplate.postForObject(paymentsUrl + "/api/payments", request, PaymentResponse.class);
        } catch (Exception e) {
            return new PaymentResponse("ERR-" + System.currentTimeMillis(), "FAILED", e.getMessage());
        }
    }

    public PaymentResponse refundPayment(String paymentId) {
        try {
            return restTemplate.postForObject(paymentsUrl + "/api/payments/" + paymentId + "/refund", null, PaymentResponse.class);
        } catch (Exception e) {
            return new PaymentResponse(paymentId, "REFUND_FAILED", e.getMessage());
        }
    }
}
