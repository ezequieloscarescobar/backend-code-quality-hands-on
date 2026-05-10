package com.meli.orders.controller;

import com.meli.orders.dto.ReportData;
import com.meli.orders.report.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<ReportData> getReport() {
        return reportService.getFullReport();
    }

    @GetMapping("/status")
    public Map<String, Long> countByStatus() {
        return reportService.countByStatus();
    }

    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Double>> getRevenue() {
        return ResponseEntity.ok(Map.of(
                "totalRevenue", reportService.getTotalRevenue(),
                "totalTaxes", reportService.getTotalTaxes()
        ));
    }

    @GetMapping("/paid")
    public List<ReportData> getPaidOrders() {
        return reportService.getPaidOrders();
    }

    @GetMapping("/range")
    public List<ReportData> getByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return reportService.getReportByDateRange(from, to);
    }
}
