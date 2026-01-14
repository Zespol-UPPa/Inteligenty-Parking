package com.smartparking.admin_service.service;

import com.smartparking.admin_service.client.PaymentClient;
import com.smartparking.admin_service.client.ParkingClient;
import com.smartparking.admin_service.dto.HourlyRevenueDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
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
    @Autowired
    private RestTemplate restTemplate;

    private static final Logger log = LoggerFactory.getLogger(FinancialReportService.class);

    private final PaymentClient paymentClient;
    private final ParkingClient parkingClient;

    @Value("${payment.service.url:http://localhost:8082}")
    private String paymentServiceUrl;

    @Value("${parking.service.url:http://localhost:8083}")
    private String parkingServiceUrl;

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
     * UPDATED: Now supports all periods including "semester"
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
                log.info("No sessions found - returning empty time slots");
                return createEmptyRevenueTimeSeries(period, startDate, endDate);
            }

            log.info("Found {} sessions", sessionIds.size());

            // 2. Get payments
            List<Map<String, Object>> payments = paymentClient.getPaymentsBySessionIds(sessionIds);

            log.info("Found {} payments", payments.size());

            // 3. Group by period
            return groupPaymentsByPeriod(payments, period, startDate, endDate);

        } catch (Exception e) {
            log.error("Error getting revenue over time", e);
            return createEmptyRevenueTimeSeries(period, startDate, endDate);
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
                            .filter(p -> "Paid".equals(p.get("status")) || "Paid".equals(p.get("statusPaid")))
                            .mapToDouble(p -> {
                                double amount = getDoubleValue(p, "amount");
                                if (amount == 0.0) {
                                    // Try amountMinor field
                                    Object amountMinor = p.get("amountMinor");
                                    if (amountMinor == null) amountMinor = p.get("amount_minor");
                                    if (amountMinor instanceof Number) {
                                        amount = ((Number) amountMinor).doubleValue() / 100.0;
                                    }
                                }
                                return amount;
                            })
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
                        .filter(p -> {
                            String paymentStatus = (String) p.get("status");
                            if (paymentStatus == null) paymentStatus = (String) p.get("statusPaid");
                            return status.equals(paymentStatus);
                        })
                        .collect(Collectors.toList());
            }

            // 4. Enrich with parking info
            Map<Long, String> parkingNames = new HashMap<>();
            for (Map<String, Object> payment : payments) {
                Long sessionId = getLongValue(payment, "sessionId");
                if (sessionId != null && sessionId > 0 && !parkingNames.containsKey(sessionId)) {
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
    // NEW WRAPPER METHODS FOR ADMINCONTROLLER
    // ========================================

    /**
     * Wrapper method for compatibility with AdminController
     * This method signature is called by the new endpoints
     */
    public Map<String, Object> getFinancialSummary(
            Long parkingId,
            Long companyId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        log.info("getFinancialSummary (wrapper) - parkingId: {}, companyId: {}", parkingId, companyId);

        if (parkingId != null) {
            return getParkingFinancialSummary(parkingId, startDate, endDate);
        }

        return getCompanyFinancialSummary(companyId, startDate, endDate);
    }

    /**
     * Wrapper method for compatibility with AdminController
     * This method signature is called by the revenue distribution endpoint
     */
    public List<Map<String, Object>> getRevenueDistributionByParking(
            List<Long> parkingIds,
            Long companyId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        log.info("getRevenueDistributionByParking (wrapper) for company {}", companyId);
        return getRevenueDistribution(companyId, startDate, endDate);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Calculate financial metrics from payment list
     * UPDATED: Better handling of different field names
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
        int totalTransactions = 0;
        double totalPaidAmount = 0.0;

        for (Map<String, Object> payment : payments) {
            try {
                // Get amount (try different field names)
                double amount = getDoubleValue(payment, "amount");
                if (amount == 0.0) {
                    Object amountMinor = payment.get("amountMinor");
                    if (amountMinor == null) amountMinor = payment.get("amount_minor");
                    if (amountMinor instanceof Number) {
                        amount = ((Number) amountMinor).doubleValue() / 100.0;
                    }
                }

                // Get status (try different field names)
                String status = (String) payment.get("status");
                if (status == null) status = (String) payment.get("statusPaid");
                if (status == null) status = (String) payment.get("status_paid");

                String activity = (String) payment.get("activity");

                if ("Paid".equalsIgnoreCase(status)) {
                    totalRevenue += amount;
                    totalTransactions++;
                    totalPaidAmount += amount;

                    if ("reservation".equalsIgnoreCase(activity) || "reservation_fee".equalsIgnoreCase(activity)) {
                        reservationFees += amount;
                    } else if (!"deposit".equalsIgnoreCase(activity)) {
                        parkingUsage += amount;
                    }
                } else if ("Pending".equalsIgnoreCase(status)) {
                    pendingPayments += amount;
                }
            } catch (Exception e) {
                log.warn("Error processing payment in metrics: {}", e.getMessage());
            }
        }

        double avgTransaction = totalTransactions > 0
                ? totalPaidAmount / totalTransactions
                : 0.0;

        metrics.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);
        metrics.put("parkingUsage", Math.round(parkingUsage * 100.0) / 100.0);
        metrics.put("pendingPayments", Math.round(pendingPayments * 100.0) / 100.0);
        metrics.put("reservationFees", Math.round(reservationFees * 100.0) / 100.0);
        metrics.put("totalTransactions", totalTransactions);
        metrics.put("avgTransactionValue", Math.round(avgTransaction * 100.0) / 100.0);

        // Calculate growth
        double growth = calculateGrowth(payments, startDate, endDate);
        metrics.put("revenueGrowth", growth);

        return metrics;
    }

    /**
     * Group payments by time period for charts
     * UPDATED: Proper handling of all periods with time slots
     */
    private List<Map<String, Object>> groupPaymentsByPeriod(
            List<Map<String, Object>> payments,
            String period,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        log.info("Grouping {} payments by period: {}", payments.size(), period);

        // Create time slots based on period
        List<String> timeSlots = createTimeSlots(period, startDate, endDate);

        // Initialize all slots with 0
        Map<String, PeriodData> grouped = new LinkedHashMap<>();
        for (String slot : timeSlots) {
            grouped.put(slot, new PeriodData());
        }

        // Group payments
        for (Map<String, Object> payment : payments) {
            try {
                // Try different field names (compatibility)
                Object dateObj = payment.get("dateTransaction");
                if (dateObj == null) dateObj = payment.get("date_transaction");
                if (dateObj == null) continue;

                LocalDateTime date;
                if (dateObj instanceof String) {
                    date = LocalDateTime.parse((String) dateObj);
                } else if (dateObj instanceof LocalDateTime) {
                    date = (LocalDateTime) dateObj;
                } else {
                    continue;
                }

                String periodKey = getPeriodKey(date, period);

                PeriodData data = grouped.get(periodKey);
                if (data == null) {
                    // Period not in our slots - skip
                    continue;
                }

                // Try different field names for amount
                double amount = getDoubleValue(payment, "amount");
                if (amount == 0.0) {
                    Object amountMinor = payment.get("amountMinor");
                    if (amountMinor == null) amountMinor = payment.get("amount_minor");
                    if (amountMinor instanceof Number) {
                        amount = ((Number) amountMinor).doubleValue() / 100.0;
                    }
                }

                // Try different field names for status
                String status = (String) payment.get("status");
                if (status == null) status = (String) payment.get("statusPaid");
                if (status == null) status = (String) payment.get("status_paid");

                String activity = (String) payment.get("activity");

                // Only count paid transactions
                if ("Paid".equalsIgnoreCase(status)) {
                    data.totalRevenue += amount;

                    // Parking usage = charges for parking (not deposits or reservation fees)
                    if (!"deposit".equalsIgnoreCase(activity) &&
                            !"reservation".equalsIgnoreCase(activity) &&
                            !"reservation_fee".equalsIgnoreCase(activity)) {
                        data.parkingUsage += amount;
                    }
                }
            } catch (Exception e) {
                log.warn("Error processing payment: {}", e.getMessage());
            }
        }

        // Convert to list
        List<Map<String, Object>> result = new ArrayList<>();
        for (String periodKey : timeSlots) {
            PeriodData data = grouped.get(periodKey);
            Map<String, Object> item = new HashMap<>();
            item.put("period", periodKey);
            item.put("totalRevenue", Math.round(data.totalRevenue * 100.0) / 100.0);
            item.put("parkingUsage", Math.round(data.parkingUsage * 100.0) / 100.0);
            result.add(item);
        }

        log.info("Grouped into {} time periods", result.size());
        return result;
    }

    /**
     * NEW: Create time slots for a period
     * This ensures we always return data for all time slots, even if some are 0
     */
    private List<String> createTimeSlots(String period, LocalDateTime startDate, LocalDateTime endDate) {
        List<String> slots = new ArrayList<>();

        switch (period.toLowerCase()) {
            case "today":
                // Hourly slots: 00:00, 01:00, ..., 23:00
                for (int hour = 0; hour < 24; hour++) {
                    slots.add(String.format("%02d:00", hour));
                }
                break;

            case "week":
                // Daily slots: Mon, Tue, Wed, Thu, Fri, Sat, Sun
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                slots.addAll(Arrays.asList(days));
                break;

            case "month":
                // Daily slots: 01, 02, 03, ... (based on current month)
                int daysInMonth = endDate.toLocalDate().lengthOfMonth();
                for (int day = 1; day <= daysInMonth; day++) {
                    slots.add(String.format("%02d", day));
                }
                break;

            case "semester":
                // Monthly slots for last 6 months
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM");
                LocalDateTime current = startDate.withDayOfMonth(1);
                while (!current.isAfter(endDate)) {
                    slots.add(current.format(formatter));
                    current = current.plusMonths(1);
                }
                break;

            case "year":
                // Monthly slots: Jan, Feb, Mar, ..., Dec
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                slots.addAll(Arrays.asList(months));
                break;

            default:
                // Fallback to hourly
                for (int hour = 0; hour < 24; hour++) {
                    slots.add(String.format("%02d:00", hour));
                }
        }

        return slots;
    }

    /**
     * NEW: Get period key for a datetime
     */
    private String getPeriodKey(LocalDateTime dateTime, String period) {
        switch (period.toLowerCase()) {
            case "today":
                return String.format("%02d:00", dateTime.getHour());

            case "week":
                return dateTime.getDayOfWeek().toString().substring(0, 3);

            case "month":
                return String.format("%02d", dateTime.getDayOfMonth());

            case "semester":
            case "year":
                return dateTime.format(DateTimeFormatter.ofPattern("MMM"));

            default:
                return String.format("%02d:00", dateTime.getHour());
        }
    }

    /**
     * NEW: Create empty revenue time series (when no data available)
     */
    private List<Map<String, Object>> createEmptyRevenueTimeSeries(String period, LocalDateTime startDate, LocalDateTime endDate) {
        List<String> slots = createTimeSlots(period, startDate, endDate);
        List<Map<String, Object>> result = new ArrayList<>();

        for (String slot : slots) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("period", slot);
            dataPoint.put("totalRevenue", 0.0);
            dataPoint.put("parkingUsage", 0.0);
            result.add(dataPoint);
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
                Object dateObj = payment.get("dateTransaction");
                if (dateObj == null) dateObj = payment.get("date_transaction");
                if (dateObj == null) continue;

                LocalDateTime date;
                if (dateObj instanceof String) {
                    date = LocalDateTime.parse((String) dateObj);
                } else if (dateObj instanceof LocalDateTime) {
                    date = (LocalDateTime) dateObj;
                } else {
                    continue;
                }

                double amount = getDoubleValue(payment, "amount");
                if (amount == 0.0) {
                    Object amountMinor = payment.get("amountMinor");
                    if (amountMinor == null) amountMinor = payment.get("amount_minor");
                    if (amountMinor instanceof Number) {
                        amount = ((Number) amountMinor).doubleValue() / 100.0;
                    }
                }

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
     * UPDATED: Added support for semester
     */
    private DateTimeFormatter getFormatterForPeriod(String period) {
        return switch (period.toLowerCase()) {
            case "today" -> DateTimeFormatter.ofPattern("HH:00");
            case "week" -> DateTimeFormatter.ofPattern("EEE");
            case "month" -> DateTimeFormatter.ofPattern("dd");
            case "semester" -> DateTimeFormatter.ofPattern("MMM");
            case "year" -> DateTimeFormatter.ofPattern("MMM");
            default -> DateTimeFormatter.ofPattern("HH:00");
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

    // ========================================
    // EXISTING HOURLY REVENUE METHODS
    // (Keep all existing methods below)
    // ========================================

    /**
     * Get hourly revenue data for a company (admin view)
     */
    public List<HourlyRevenueDTO> getHourlyRevenueForCompany(Long companyId, String period) {
        LocalDateTime[] dateRange = getDateRange(period);
        LocalDateTime startDate = dateRange[0];
        LocalDateTime endDate = dateRange[1];

        List<Long> sessionIds = getSessionIdsFromParkingService(companyId, null, startDate, endDate);

        if (sessionIds.isEmpty()) {
            System.out.println("No sessions found for company " + companyId + " in period " + period);
            return new ArrayList<>();
        }

        System.out.println("Found " + sessionIds.size() + " sessions for company " + companyId);

        List<Map<String, Object>> payments = getPaymentsFromPaymentService(sessionIds);

        System.out.println("Found " + payments.size() + " payments for " + sessionIds.size() + " sessions");

        Map<String, Double> hourlyRevenue = new HashMap<>();

        for (Map<String, Object> payment : payments) {
            Object dateObj = payment.get("date_transaction");
            Object amountObj = payment.get("amount_minor");
            Object statusObj = payment.get("status_paid");
            Object activityObj = payment.get("activity");

            if (statusObj != null && "Paid".equalsIgnoreCase(statusObj.toString()) &&
                    activityObj != null && "parking".equalsIgnoreCase(activityObj.toString())) {

                if (dateObj != null && amountObj != null) {
                    String hour = extractHour(dateObj);
                    Double amount = ((Number) amountObj).doubleValue() / 100.0;

                    hourlyRevenue.merge(hour, amount, Double::sum);
                }
            }
        }

        return hourlyRevenue.entrySet().stream()
                .map(e -> new HourlyRevenueDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(HourlyRevenueDTO::getHour))
                .collect(Collectors.toList());
    }

    /**
     * Get hourly revenue data for a specific parking (worker view)
     */
    public List<HourlyRevenueDTO> getHourlyRevenueForParking(Long parkingId, String period) {
        LocalDateTime[] dateRange = getDateRange(period);
        LocalDateTime startDate = dateRange[0];
        LocalDateTime endDate = dateRange[1];

        List<Long> sessionIds = getSessionIdsFromParkingService(null, parkingId, startDate, endDate);

        if (sessionIds.isEmpty()) {
            System.out.println("No sessions found for parking " + parkingId + " in period " + period);
            return new ArrayList<>();
        }

        System.out.println("Found " + sessionIds.size() + " sessions for parking " + parkingId);

        List<Map<String, Object>> payments = getPaymentsFromPaymentService(sessionIds);

        System.out.println("Found " + payments.size() + " payments for " + sessionIds.size() + " sessions");

        Map<String, Double> hourlyRevenue = new HashMap<>();

        for (Map<String, Object> payment : payments) {
            Object dateObj = payment.get("date_transaction");
            Object amountObj = payment.get("amount_minor");
            Object statusObj = payment.get("status_paid");
            Object activityObj = payment.get("activity");

            if (statusObj != null && "Paid".equalsIgnoreCase(statusObj.toString()) &&
                    activityObj != null && "parking".equalsIgnoreCase(activityObj.toString())) {

                if (dateObj != null && amountObj != null) {
                    String hour = extractHour(dateObj);
                    Double amount = ((Number) amountObj).doubleValue() / 100.0;

                    hourlyRevenue.merge(hour, amount, Double::sum);
                }
            }
        }

        return hourlyRevenue.entrySet().stream()
                .map(e -> new HourlyRevenueDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(HourlyRevenueDTO::getHour))
                .collect(Collectors.toList());
    }

    /**
     * Get session IDs from parking-service
     */
    private List<Long> getSessionIdsFromParkingService(Long companyId, Long parkingId,
                                                       LocalDateTime startDate, LocalDateTime endDate) {
        try {
            String url;
            UriComponentsBuilder builder;

            if (companyId != null) {
                url = parkingServiceUrl + "/parking/sessions/ids-by-company";
                builder = UriComponentsBuilder.fromHttpUrl(url)
                        .queryParam("companyId", companyId)
                        .queryParam("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .queryParam("endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                url = parkingServiceUrl + "/parking/sessions/ids-by-parking";
                builder = UriComponentsBuilder.fromHttpUrl(url)
                        .queryParam("parkingId", parkingId)
                        .queryParam("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .queryParam("endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            String finalUrl = builder.toUriString();
            System.out.println("Calling parking-service: GET " + finalUrl);

            ResponseEntity<List<Long>> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Long>>() {}
            );

            if (response.getBody() != null) {
                return response.getBody();
            }

            return new ArrayList<>();

        } catch (Exception e) {
            System.err.println("Failed to fetch session IDs from parking-service: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get payments from payment-service using session IDs
     */
    private List<Map<String, Object>> getPaymentsFromPaymentService(List<Long> sessionIds) {
        try {
            String url = paymentServiceUrl + "/payment/by-sessions";
            System.out.println("Calling payment-service: POST " + url + " with " + sessionIds.size() + " session IDs");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<List<Long>> request = new HttpEntity<>(sessionIds, headers);

            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    List.class
            );

            if (response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody();
            }

            return new ArrayList<>();

        } catch (Exception e) {
            System.err.println("Failed to fetch payments from payment-service: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Extract hour from date object
     */
    private String extractHour(Object dateObj) {
        try {
            if (dateObj instanceof String) {
                String dateStr = (String) dateObj;
                if (dateStr.contains("T")) {
                    LocalDateTime dt = LocalDateTime.parse(dateStr);
                    return String.format("%02d:00", dt.getHour());
                } else if (dateStr.contains(" ")) {
                    String[] parts = dateStr.split(" ");
                    if (parts.length > 1 && parts[1].contains(":")) {
                        String[] timeParts = parts[1].split(":");
                        return String.format("%s:00", timeParts[0]);
                    }
                }
            } else if (dateObj instanceof LocalDateTime) {
                LocalDateTime dt = (LocalDateTime) dateObj;
                return String.format("%02d:00", dt.getHour());
            }
        } catch (Exception e) {
            System.err.println("Failed to parse date: " + dateObj + " - " + e.getMessage());
        }

        return "00:00";
    }

    /**
     * Convert period string to date range
     */
    private LocalDateTime[] getDateRange(String period) {
        LocalDate date;

        if (period == null || period.isEmpty() || period.equalsIgnoreCase("today")) {
            date = LocalDate.now();
        } else if (period.equalsIgnoreCase("yesterday")) {
            date = LocalDate.now().minusDays(1);
        } else if (period.matches("\\d{4}-\\d{2}-\\d{2}")) {
            date = LocalDate.parse(period);
        } else {
            date = LocalDate.now();
        }

        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.atTime(23, 59, 59);

        return new LocalDateTime[]{startDate, endDate};
    }

    /**
     * Get hourly revenue for last 7 days
     */
    public List<HourlyRevenueDTO> getWeeklyRevenueForCompany(Long companyId) {
        LocalDateTime endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        LocalDateTime startDate = LocalDateTime.now().minusDays(7).withHour(0).withMinute(0).withSecond(0);

        List<Long> sessionIds = getSessionIdsFromParkingService(companyId, null, startDate, endDate);

        if (sessionIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> payments = getPaymentsFromPaymentService(sessionIds);

        Map<String, Double> hourlyRevenue = new HashMap<>();

        for (Map<String, Object> payment : payments) {
            Object dateObj = payment.get("date_transaction");
            Object amountObj = payment.get("amount_minor");
            Object statusObj = payment.get("status_paid");
            Object activityObj = payment.get("activity");

            if (statusObj != null && "Paid".equalsIgnoreCase(statusObj.toString()) &&
                    activityObj != null && "parking".equalsIgnoreCase(activityObj.toString())) {

                if (dateObj != null && amountObj != null) {
                    String dateHour = extractDateAndHour(dateObj);
                    Double amount = ((Number) amountObj).doubleValue() / 100.0;

                    hourlyRevenue.merge(dateHour, amount, Double::sum);
                }
            }
        }

        return hourlyRevenue.entrySet().stream()
                .map(e -> new HourlyRevenueDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(HourlyRevenueDTO::getHour))
                .collect(Collectors.toList());
    }

    /**
     * Extract date and hour from date object
     */
    private String extractDateAndHour(Object dateObj) {
        try {
            if (dateObj instanceof String) {
                String dateStr = (String) dateObj;
                if (dateStr.contains("T")) {
                    LocalDateTime dt = LocalDateTime.parse(dateStr);
                    return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));
                } else if (dateStr.contains(" ")) {
                    String[] parts = dateStr.split(" ");
                    if (parts.length > 1 && parts[1].contains(":")) {
                        String[] timeParts = parts[1].split(":");
                        return parts[0] + " " + timeParts[0] + ":00";
                    }
                }
            } else if (dateObj instanceof LocalDateTime) {
                LocalDateTime dt = (LocalDateTime) dateObj;
                return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));
            }
        } catch (Exception e) {
            System.err.println("Failed to parse date: " + dateObj + " - " + e.getMessage());
        }

        return "2026-01-01 00:00";
    }
}