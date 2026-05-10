package com.meli.orders.client;

import com.meli.orders.dto.InventoryRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url}")
    private String inventoryUrl;

    public InventoryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean reserveStock(String productId, Integer quantity) {
        try {
            InventoryRequest req = new InventoryRequest(productId, quantity, "RESERVE");
            restTemplate.postForObject(inventoryUrl + "/api/inventory/reserve", req, Void.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean releaseStock(String productId, Integer quantity) {
        try {
            InventoryRequest req = new InventoryRequest(productId, quantity, "RELEASE");
            restTemplate.postForObject(inventoryUrl + "/api/inventory/release", req, Void.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean confirmStock(String productId, Integer quantity) {
        try {
            InventoryRequest req = new InventoryRequest(productId, quantity, "CONFIRM");
            restTemplate.postForObject(inventoryUrl + "/api/inventory/confirm", req, Void.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
