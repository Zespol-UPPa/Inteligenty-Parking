package com.smartparking.admin_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PaymentClient - Communication with payment-service
 * Used by admin-service to fetch financial data for reports
 *
 * UPDATED VERSION - includes getPaymentsBySessionIds for separate databases
 */
@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PaymentClient(RestTemplate restTemplate,
                         @Value("${PAYMENT_SERVICE_URL:http://payment-service:8082}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * ========================================
     * CRITICAL METHOD FOR SEPARATE DATABASES
     * ========================================
     * Get payments by list of session IDs
     * This is used by FinancialReportService to get payments for specific sessions
     *
     * @param sessionIds - list of session IDs from parking-service
     * @return List of payment data
     */
    public List<Map<String, Object>> getPaymentsBySessionIds(List<Long> sessionIds) {
        try {
            if (sessionIds == null || sessionIds.isEmpty()) {
                log.warn("Empty session IDs list provided");
                return new ArrayList<>();
            }

            String url = baseUrl + "/payment/by-sessions";

            log.info("Requesting payments for {} sessions from payment-service", sessionIds.size());

            ResponseEntity<List> response = restTemplate.postForEntity(
                    url,
                    sessionIds,  // POST body - list of session IDs
                    List.class
            );

            List<?> body = response.getBody();
            if (body != null) {
                log.info("Received {} payments from payment-service", body.size());
                return (List<Map<String, Object>>) body;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Failed to get payments by session IDs: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get financial summary for a specific parking
     * Returns total revenue, transaction count, pending payments, etc.
     *
     * @param parkingId - parking location ID
     * @param timePeriod - "today", "week", "month", "quarter", "year"
     * @return Map with financial metrics
     */
    public Map<String, Object> getParkingFinancialSummary(Long parkingId, String timePeriod) {
        try {
            String url = baseUrl + "/payment/admin/reports/parking/{parkingId}/summary?period={period}";

            ResponseEntity<Map> response = restTemplate.getForEntity(
                    url,
                    Map.class,
                    parkingId,
                    timePeriod
            );

            return response.getBody() != null ? response.getBody() : new HashMap<>();

        } catch (Exception e) {
            log.error("Failed to get parking financial summary for parking " + parkingId + ": " + e.getMessage());
            return createEmptyFinancialSummary();
        }
    }

    /**
     * Get financial summary for ALL parkings in a company
     *
     * @param companyId - company ID
     * @param timePeriod - "today", "week", "month", "quarter", "year"
     * @return Map with aggregated financial metrics
     */
    public Map<String, Object> getCompanyFinancialSummary(Long companyId, String timePeriod) {
        try {
            String url = baseUrl + "/payment/admin/reports/company/{companyId}/summary?period={period}";

            ResponseEntity<Map> response = restTemplate.getForEntity(
                    url,
                    Map.class,
                    companyId,
                    timePeriod
            );

            return response.getBody() != null ? response.getBody() : new HashMap<>();

        } catch (Exception e) {
            log.error("Failed to get company financial summary for company " + companyId + ": " + e.getMessage());
            return createEmptyFinancialSummary();
        }
    }

    /**
     * Get revenue over time (for charts)
     * Returns daily/weekly/monthly revenue data
     *
     * @param parkingId - parking ID (null for all parkings in company)
     * @param companyId - company ID
     * @param timePeriod - "week", "month", "quarter", "year"
     * @return List of revenue data points
     */
    public List<Map<String, Object>> getRevenueOverTime(Long parkingId, Long companyId, String timePeriod) {
        try {
            String url;
            Map<String, Object> params = new HashMap<>();
            params.put("period", timePeriod);

            if (parkingId != null) {
                url = baseUrl + "/payment/admin/reports/parking/{parkingId}/revenue-over-time?period={period}";
                params.put("parkingId", parkingId);
            } else {
                url = baseUrl + "/payment/admin/reports/company/{companyId}/revenue-over-time?period={period}";
                params.put("companyId", companyId);
            }

            ResponseEntity<List> response = restTemplate.getForEntity(
                    url,
                    List.class,
                    params
            );

            return response.getBody() != null ? response.getBody() : new ArrayList<>();

        } catch (Exception e) {
            log.error("Failed to get revenue over time: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get revenue distribution by parking (for pie chart)
     * Only for admins viewing all parkings
     *
     * @param companyId - company ID
     * @param timePeriod - "today", "week", "month", "quarter", "year"
     * @return List of parking revenue breakdowns
     */
    public List<Map<String, Object>> getRevenueDistribution(Long companyId, String timePeriod) {
        try {
            String url = baseUrl + "/payment/admin/reports/company/{companyId}/revenue-distribution?period={period}";

            ResponseEntity<List> response = restTemplate.getForEntity(
                    url,
                    List.class,
                    companyId,
                    timePeriod
            );

            return response.getBody() != null ? response.getBody() : new ArrayList<>();

        } catch (Exception e) {
            log.error("Failed to get revenue distribution for company " + companyId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get detailed transaction list
     *
     * @param parkingId - parking ID (null for all parkings)
     * @param companyId - company ID
     * @param timePeriod - "today", "week", "month", "quarter", "year"
     * @param status - "all", "paid", "pending", "failed" (optional)
     * @return List of transactions
     */
    public List<Map<String, Object>> getTransactions(Long parkingId, Long companyId, String timePeriod, String status) {
        try {
            String url;
            Map<String, Object> params = new HashMap<>();
            params.put("period", timePeriod);

            if (status != null && !status.equals("all")) {
                params.put("status", status);
            }

            if (parkingId != null) {
                url = baseUrl + "/payment/admin/reports/parking/{parkingId}/transactions?period={period}";
                if (status != null && !status.equals("all")) {
                    url += "&status={status}";
                }
                params.put("parkingId", parkingId);
            } else {
                url = baseUrl + "/payment/admin/reports/company/{companyId}/transactions?period={period}";
                if (status != null && !status.equals("all")) {
                    url += "&status={status}";
                }
                params.put("companyId", companyId);
            }

            ResponseEntity<List> response = restTemplate.getForEntity(
                    url,
                    List.class,
                    params
            );

            return response.getBody() != null ? response.getBody() : new ArrayList<>();

        } catch (Exception e) {
            log.error("Failed to get transactions: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Create empty financial summary (fallback when service is down)
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
}