package com.smartparking.worker_service.service;

import com.smartparking.worker_service.client.ParkingClient;
import com.smartparking.worker_service.model.Worker;
import com.smartparking.worker_service.repo.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * WorkerFinancialReportService
 *
 * Orchestrates data from payment-service and parking-service for worker's assigned parking
 * Similar to admin's FinancialReportService but worker-specific (single parking only)
 */
@Service
public class WorkerFinancialReportService {

    private static final Logger log = LoggerFactory.getLogger(WorkerFinancialReportService.class);

    private final WorkerRepository workerRepository;
    private final ParkingClient parkingClient;
    private final RestTemplate restTemplate;

    public WorkerFinancialReportService(
            WorkerRepository workerRepository,
            ParkingClient parkingClient,
            RestTemplate restTemplate) {
        this.workerRepository = workerRepository;
        this.parkingClient = parkingClient;
        this.restTemplate = restTemplate;
    }

    /**
     * Get financial summary for worker's assigned parking
     */
    public Map<String, Object> getFinancialSummary(Long accountId, String period) {
        log.info("Getting financial summary for worker accountId={}, period={}", accountId, period);

        // 1. Get worker and their parking
        Optional<Worker> workerOpt = workerRepository.findByAccountId(accountId);
        if (workerOpt.isEmpty()) {
            log.warn("Worker not found for accountId: {}", accountId);
            return createEmptyFinancialSummary();
        }

        Worker worker = workerOpt.get();
        Long parkingId = worker.getRefParkingId();

        if (parkingId == null) {
            log.warn("Worker {} has no parking assigned", accountId);
            return createEmptyFinancialSummary();
        }

        log.info("Worker's parking ID: {}", parkingId);

        // 2. Calculate date range
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = calculateStartDate(endDate, period);

        log.info("Date range: {} to {}", startDate, endDate);

        // 3. Get session IDs from parking-service
        List<Long> sessionIds = parkingClient.getSessionIdsByParkingId(parkingId, startDate, endDate);

        if (sessionIds.isEmpty()) {
            log.warn("No sessions found for parking {} in period", parkingId);
            return createEmptyFinancialSummary();
        }

        log.info("Found {} sessions for parking {}", sessionIds.size(), parkingId);

        // 4. Get payments from payment-service
        List<Map<String, Object>> payments = getPaymentsBySessionIds(sessionIds);

        log.info("Found {} payments for sessions", payments.size());

        // 5. Calculate metrics
        Map<String, Object> summary = calculateFinancialMetrics(payments, startDate, endDate);

        log.info("Financial summary calculated successfully");
        return summary;
    }

    /**
     * Get revenue over time for charts
     */
    public List<Map<String, Object>> getRevenueOverTime(Long accountId, String period) {
        log.info("Getting revenue over time for worker accountId={}, period={}", accountId, period);

        Optional<Worker> workerOpt = workerRepository.findByAccountId(accountId);
        if (workerOpt.isEmpty()) {
            return new ArrayList<>();
        }

        Worker worker = workerOpt.get();
        Long parkingId = worker.getRefParkingId();

        if (parkingId == null) {
            return new ArrayList<>();
        }

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = calculateStartDate(endDate, period);

        List<Long> sessionIds = parkingClient.getSessionIdsByParkingId(parkingId, startDate, endDate);

        if (sessionIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> payments = getPaymentsBySessionIds(sessionIds);

        return groupPaymentsByPeriod(payments, period, startDate, endDate);
    }

    /**
     * Get transactions list
     */
    public List<Map<String, Object>> getTransactions(Long accountId, String period, String status) {
        log.info("Getting transactions for worker accountId={}, period={}, status={}", accountId, period, status);

        Optional<Worker> workerOpt = workerRepository.findByAccountId(accountId);
        if (workerOpt.isEmpty()) {
            return new ArrayList<>();
        }

        Worker worker = workerOpt.get();
        Long parkingId = worker.getRefParkingId();

        if (parkingId == null) {
            return new ArrayList<>();
        }

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = calculateStartDate(endDate, period);

        List<Long> sessionIds = parkingClient.getSessionIdsByParkingId(parkingId, startDate, endDate);

        if (sessionIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> payments = getPaymentsBySessionIds(sessionIds);

        // Filter by status
        if (status != null && !"all".equalsIgnoreCase(status)) {
            payments = payments.stream()
                    .filter(p -> status.equals(p.get("status")))
                    .collect(Collectors.toList());
        }

        // Limit to 100 transactions
        return payments.stream()
                .limit(100)
                .collect(Collectors.toList());
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private LocalDateTime calculateStartDate(LocalDateTime endDate, String period) {
        return switch (period.toLowerCase()) {
            case "today" -> endDate.toLocalDate().atStartOfDay();
            case "week" -> endDate.minusWeeks(1);
            case "month" -> endDate.minusMonths(1);
            case "quarter" -> endDate.minusMonths(3);
            case "semester" -> endDate.minusMonths(6);
            case "year" -> endDate.minusYears(1);
            default -> endDate.minusMonths(1);
        };
    }

    /**
     * Get payments from payment-service by session IDs
     */
    private List<Map<String, Object>> getPaymentsBySessionIds(List<Long> sessionIds) {
        try {
            String paymentServiceUrl = "http://payment-service:8082/payment/by-sessions";

            log.debug("Calling payment-service: {}", paymentServiceUrl);

            ResponseEntity<List> response = restTemplate.postForEntity(
                    paymentServiceUrl,
                    sessionIds,
                    List.class
            );

            if (response.getBody() == null) {
                return new ArrayList<>();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> payments = (List<Map<String, Object>>) response.getBody();

            return payments;

        } catch (Exception e) {
            log.error("Failed to get payments from payment-service", e);
            return new ArrayList<>();
        }
    }

    /**
     * Calculate financial metrics from payment list
     */
    private Map<String, Object> calculateFinancialMetrics(
            List<Map<String, Object>> payments,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        Map<String, Object> metrics = new HashMap<>();

        double totalRevenue = 0.0;
        double parkingUsage = 0.0;
        double pendingPayments = 0.0;
        double reservationFees = 0.0;
        int totalTransactions = payments.size();

        for (Map<String, Object> payment : payments) {
            double amount = getDoubleValue(payment, "amount");
            String status = (String) payment.get("status");
            String activity = (String) payment.get("activity");

            totalRevenue += amount;

            if ("Paid".equals(status) && "parking".equals(activity)) {
                parkingUsage += amount;
            }

            if ("Pending".equals(status)) {
                pendingPayments += amount;
            }

            if ("reservation".equals(activity) && "Paid".equals(status)) {
                reservationFees += amount;
            }
        }

        double avgTransaction = totalTransactions > 0 ? totalRevenue / totalTransactions : 0.0;

        metrics.put("totalRevenue", totalRevenue);
        metrics.put("parkingUsage", parkingUsage);
        metrics.put("pendingPayments", pendingPayments);
        metrics.put("reservationFees", reservationFees);
        metrics.put("totalTransactions", totalTransactions);
        metrics.put("avgTransactionValue", avgTransaction);
        metrics.put("revenueGrowth", calculateGrowth(payments, startDate, endDate));

        return metrics;
    }

    /**
     * Group payments by time period for charts
     */
    private List<Map<String, Object>> groupPaymentsByPeriod(
            List<Map<String, Object>> payments,
            String period,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        Map<String, PeriodData> grouped = new LinkedHashMap<>();
        DateTimeFormatter formatter = getFormatterForPeriod(period);

        for (Map<String, Object> payment : payments) {
            String dateStr = (String) payment.get("dateTransaction");
            if (dateStr == null) continue;

            LocalDateTime date = LocalDateTime.parse(dateStr);
            String periodKey = date.format(formatter);

            PeriodData data = grouped.computeIfAbsent(periodKey, k -> new PeriodData());

            double amount = getDoubleValue(payment, "amount");
            String status = (String) payment.get("status");
            String activity = (String) payment.get("activity");

            data.totalRevenue += amount;
            if ("Paid".equals(status) && "parking".equals(activity)) {
                data.parkingUsage += amount;
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, PeriodData> entry : grouped.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("period", entry.getKey());
            item.put("totalRevenue", entry.getValue().totalRevenue);
            item.put("parkingUsage", entry.getValue().parkingUsage);
            result.add(item);
        }

        return result;
    }

    /**
     * Calculate revenue growth %
     */
    private double calculateGrowth(
            List<Map<String, Object>> payments,
            LocalDateTime currentStart,
            LocalDateTime currentEnd) {
        try {
            long periodDays = java.time.Duration.between(currentStart, currentEnd).toDays();
            LocalDateTime previousEnd = currentStart.minusSeconds(1);
            LocalDateTime previousStart = previousEnd.minusDays(periodDays);

            double currentRevenue = 0.0;
            double previousRevenue = 0.0;

            for (Map<String, Object> payment : payments) {
                String dateStr = (String) payment.get("dateTransaction");
                if (dateStr == null) continue;

                LocalDateTime date = LocalDateTime.parse(dateStr);
                double amount = getDoubleValue(payment, "amount");

                if (date.isAfter(currentStart) && date.isBefore(currentEnd)) {
                    currentRevenue += amount;
                } else if (date.isAfter(previousStart) && date.isBefore(previousEnd)) {
                    previousRevenue += amount;
                }
            }

            if (previousRevenue > 0) {
                return ((currentRevenue - previousRevenue) / previousRevenue) * 100.0;
            }
            return 0.0;
        } catch (Exception e) {
            log.error("Error calculating growth", e);
            return 0.0;
        }
    }

    private DateTimeFormatter getFormatterForPeriod(String period) {
        return switch (period.toLowerCase()) {
            case "day", "week" -> DateTimeFormatter.ofPattern("MMM dd");
            case "month" -> DateTimeFormatter.ofPattern("MMM");
            case "quarter", "year", "semester" -> DateTimeFormatter.ofPattern("MMM yyyy");
            default -> DateTimeFormatter.ofPattern("MMM dd");
        };
    }

    private Map<String, Object> createEmptyFinancialSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRevenue", 0.0);
        summary.put("parkingUsage", 0.0);
        summary.put("pendingPayments", 0.0);
        summary.put("reservationFees", 0.0);
        summary.put("totalTransactions", 0);
        summary.put("avgTransactionValue", 0.0);
        summary.put("revenueGrowth", 0.0);
        return summary;
    }

    private double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private static class PeriodData {
        double totalRevenue = 0.0;
        double parkingUsage = 0.0;
    }
}