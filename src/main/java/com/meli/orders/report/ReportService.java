package com.meli.orders.report;

import com.meli.orders.dto.ReportData;
import com.meli.orders.model.Order;
import com.meli.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<ReportData> getFullReport() {
        return orderRepository.findAll().stream()
                .map(o -> new ReportData(
                        o.getId(),
                        o.getStatus(),
                        o.getCustomerId(),
                        o.getTotal(),
                        o.getTaxes(),
                        o.getCreatedAt(),
                        o.getItems().size()
                ))
                .collect(Collectors.toList());
    }

    public Map<String, Long> countByStatus() {
        return orderRepository.findAll().stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
    }

    public double getTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus().equals("PAID"))
                .mapToDouble(Order::getTotal)
                .sum();
    }

    public double getTotalTaxes() {
        List<Order> all = orderRepository.findAll();
        double taxes = 0;
        for (Order o : all) {
            if (o.getStatus().equals("PAID") && o.getTaxes() != null) {
                taxes += o.getTotal() * 0.21;
            }
        }
        return taxes;
    }

    public List<ReportData> getReportByDateRange(LocalDateTime from, LocalDateTime to) {
        return orderRepository.findByDateRange(from, to).stream()
                .map(o -> new ReportData(
                        o.getId(),
                        o.getStatus(),
                        o.getCustomerName(),
                        o.getSubtotal(),
                        o.getTaxes(),
                        o.getCreatedAt(),
                        o.getItems().stream().mapToInt(i -> i.getQty()).sum()
                ))
                .collect(Collectors.toList());
    }

    public List<ReportData> getPaidOrders() {
        return orderRepository.findByStatus("PAID").stream()
                .map(o -> new ReportData(
                        o.getId(),
                        o.getStatus(),
                        o.getCustomerId(),
                        o.getTotal(),
                        o.getTotal() * 0.21,
                        o.getPaidAt(),
                        o.getItems().size()
                ))
                .collect(Collectors.toList());
    }
}
