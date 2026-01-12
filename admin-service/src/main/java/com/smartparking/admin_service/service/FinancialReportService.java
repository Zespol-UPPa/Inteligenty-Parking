package com.smartparking.admin_service.service;

import com.smartparking.admin_service.client.PaymentClient;
import com.smartparking.admin_service.client.ParkingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FinancialReportService
 *
 * Orchestrates data from payment-service and parking-service
 * Aggregates financial metrics for financial reports
 *
 * This is necessary because Payment_DB and PARKING_DB are separate databases
 */
@Service
public class FinancialReportService {

    private static final Logger log = LoggerFactory.getLogger(FinancialReportService.class);

    private final PaymentClient paymentClient;
    private final ParkingClient parkingClient;

    public FinancialReportService(PaymentClient paymentClient, ParkingClient parkingClient) {
        this.paymentClient = paymentClient;
        this.parkingClient = parkingClient;
    }

    /**
     * Get financial summary for a specific parking
     */
    public Map<String, Object> getParkingFinancialSummary(
            Long parkingId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        log.info("Getting financial summary for parking {}", parkingId);

        try {
            // 1. Get session IDs from parking-service
            List<Long> sessionIds = parkingClient.getSessionIdsByParkingId(
                    parkingId, startDate, endDate);

            if (sessionIds.isEmpty()) {
                log.warn("No sessions found for parking {} in period", parkingId);
                return createEmptyFinancialSummary();
            }

            log.info("Found {} sessions for parking {}", sessionIds.size(), parkingId);

            // 2. Get payments for these sessions from payment-service
            List<Map<String, Object>> payments = paymentClient.getPaymentsBySessionIds(sessionIds);

            log.info("Found {} payments for sessions", payments.size());

            // 3. Calculate metrics
            return calculateFinancialMetrics(payments, startDate, endDate);

        } catch (Exception e) {
            log.error("Error getting financial summary for parking " + parkingId, e);
            return createEmptyFinancialSummary();
        }
    }

    /**
     * Get financial summary for all parkings in a company
     */
    public Map<String, Object> getCompanyFinancialSummary(
            Long companyId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        log.info("Getting financial summary for company {}", companyId);

        try {
            List<Long> sessionIds = parkingClient.getSessionIdsByCompanyId(
                    companyId, startDate, endDate);

            if (sessionIds.isEmpty()) {
                log.warn("No sessions found for company {} in period", companyId);
                return createEmptyFinancialSummary();
            }

            log.info("Found {} sessions for company {}", sessionIds.size(), companyId);

            List<Map<String, Object>> payments = paymentClient.getPaymentsBySessionIds(sessionIds);

            log.info("Found {} payments for company sessions", payments.size());

            return calculateFinancialMetrics(payments, startDate, endDate);

        } catch (Exception e) {
            log.error("Error getting financial summary for company " + companyId, e);
            return createEmptyFinancialSummary();
        }
    }

    /**
     * Get revenue over time (for charts)
     */
    public List<Map<String, Object>> getRevenueOverTime(
            Long parkingId,
            Long companyId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String period) {

        log.info("Getting revenue over time - parking: {}, company: {}, period: {}",
                parkingId, companyId, period);

        try {
            // 1. Get session IDs
            List<Long> sessionIds;

            if (parkingId != null) {
                sessionIds = parkingClient.getSessionIdsByParkingId(parkingId, startDate, endDate);
            } else {
                sessionIds = parkingClient.getSessionIdsByCompanyId(companyId, startDate, endDate);
            }

            if (sessionIds.isEmpty()) {
                return new ArrayList<>();
            }

            // 2. Get payments
            List<Map<String, Object>> payments = paymentClient.getPaymentsBySessionIds(sessionIds);

            // 3. Group by period
            return groupPaymentsByPeriod(payments, period, startDate, endDate);

        } catch (Exception e) {
            log.error("Error getting revenue over time", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get revenue distribution by parking (for pie chart)
     */
    public List<Map<String, Object>> getRevenueDistribution(
            Long companyId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        log.info("Getting revenue distribution for company {}", companyId);

        try {
            // 1. Get all parking IDs for company
            List<Long> parkingIds = parkingClient.getParkingIdsByCompany(companyId);

            if (parkingIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> distribution = new ArrayList<>();

            // 2. For each parking, get revenue
            for (Long parkingId : parkingIds) {
                List<Long> sessionIds = parkingClient.getSessionIdsByParkingId(
                        parkingId, startDate, endDate);

                if (!sessionIds.isEmpty()) {
                    List<Map<String, Object>> payments =
                            paymentClient.getPaymentsBySessionIds(sessionIds);

                    double revenue = payments.stream()
                            .filter(p -> "Paid".equals(p.get("status")))
                            .mapToDouble(p -> getDoubleValue(p, "amount") / 100.0)
                            .sum();

                    if (revenue > 0) {
                        String parkingName = parkingClient.getParkingName(parkingId);

                        Map<String, Object> item = new HashMap<>();
                        item.put("parkingId", parkingId);
                        item.put("parkingName", parkingName);
                        item.put("revenue", revenue);

                        distribution.add(item);
                    }
                }
            }

            // Sort by revenue descending
            distribution.sort((a, b) ->
                    Double.compare((Double) b.get("revenue"), (Double) a.get("revenue")));

            return distribution;

        } catch (Exception e) {
            log.error("Error getting revenue distribution", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get detailed transactions list
     */
    public List<Map<String, Object>> getTransactions(
            Long parkingId,
            Long companyId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String status) {

        log.info("Getting transactions - parking: {}, company: {}, status: {}",
                parkingId, companyId, status);

        try {
            // 1. Get session IDs
            List<Long> sessionIds;

            if (parkingId != null) {
                sessionIds = parkingClient.getSessionIdsByParkingId(parkingId, startDate, endDate);
            } else {
                sessionIds = parkingClient.getSessionIdsByCompanyId(companyId, startDate, endDate);
            }

            if (sessionIds.isEmpty()) {
                return new ArrayList<>();
            }

            // 2. Get payments with details
            List<Map<String, Object>> payments = paymentClient.getPaymentsBySessionIds(sessionIds);

            // 3. Filter by status if needed
            if (status != null && !"all".equalsIgnoreCase(status)) {
                payments = payments.stream()
                        .filter(p -> status.equals(p.get("status")))
                        .collect(Collectors.toList());
            }

            // 4. Enrich with parking info (get parking names for session IDs)
            Map<Long, String> parkingNames = new HashMap<>();
            for (Map<String, Object> payment : payments) {
                Long sessionId = getLongValue(payment, "sessionId");
                if (sessionId != null && sessionId > 0 && !parkingNames.containsKey(sessionId)) {
                    // Get parking info for this session
                    Long parkId = parkingClient.getParkingIdBySessionId(sessionId);
                    if (parkId != null) {
                        String name = parkingClient.getParkingName(parkId);
                        parkingNames.put(sessionId, name);
                    }
                }
            }

            // Add parking names to payments
            for (Map<String, Object> payment : payments) {
                Long sessionId = getLongValue(payment, "sessionId");
                String parkingName = parkingNames.getOrDefault(sessionId, "N/A");
                payment.put("parkingName", parkingName);
            }

            // Limit to 100 most recent
            return payments.stream()
                    .limit(100)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting transactions", e);
            return new ArrayList<>();
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

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

        double avgTransaction = totalTransactions > 0
                ? totalRevenue / totalTransactions
                : 0.0;

        metrics.put("totalRevenue", totalRevenue);
        metrics.put("parkingUsage", parkingUsage);
        metrics.put("pendingPayments", pendingPayments);
        metrics.put("reservationFees", reservationFees);
        metrics.put("totalTransactions", totalTransactions);
        metrics.put("avgTransactionValue", avgTransaction);

        // Calculate growth
        double growth = calculateGrowth(payments, startDate, endDate);
        metrics.put("revenueGrowth", growth);

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

        // Convert to list
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
            // Split payments into current and previous period
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

    /**
     * Get date formatter for period
     */
    private DateTimeFormatter getFormatterForPeriod(String period) {
        return switch (period.toLowerCase()) {
            case "day", "week" -> DateTimeFormatter.ofPattern("MMM dd");
            case "month" -> DateTimeFormatter.ofPattern("MMM");
            case "quarter", "year" -> DateTimeFormatter.ofPattern("MMM yyyy");
            default -> DateTimeFormatter.ofPattern("MMM dd");
        };
    }

    /**
     * Create empty financial summary
     */
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

    /**
     * Safely get double value from map
     */
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

    /**
     * Safely get long value from map
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Helper class for grouping period data
     */
    private static class PeriodData {
        double totalRevenue = 0.0;
        double parkingUsage = 0.0;
    }
}