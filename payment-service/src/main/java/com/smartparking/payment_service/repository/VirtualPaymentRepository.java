package com.smartparking.payment_service.repository;
import com.smartparking.payment_service.model.VirtualPayment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
public interface VirtualPaymentRepository {
    Optional<VirtualPayment> findById(Long id);
    List<VirtualPayment> findByAccountId(Long accountId);
    List<VirtualPayment> findBySessionId(Long sessionId);
    List<VirtualPayment> findAll();
    VirtualPayment save(VirtualPayment payment);
    List<VirtualPayment> findByAccountIdAndActivity(Long accountId, String activity);
    List<VirtualPayment> findByAccountIdAndDateRange(Long accountId, LocalDateTime from, LocalDateTime to);
    Long sumByAccountId(Long accountId);                              // suma płatności klienta
    Long sumByAccountIdAndActivity(Long accountId, String activity);  // suma wg typu aktywności
    Long sumAllPaid();                                                // suma wszystkich płatności (status = PAID)
    Long countByAccountId(Long accountId);                            // liczba płatności klienta
    Long countAll();

    // FINANCIAL REPORT METHODS
    /**
     * Find payments by list of session IDs
     * Used when admin-service provides session IDs from parking-service
     */
    List<VirtualPayment> findBySessionIds(List<Long> sessionIds);

    /**
     * Find all payments in date range
     */
    List<VirtualPayment> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get payment summary metrics (no parking filtering)
     * Returns: totalRevenue, parkingRevenue, pendingPayments, reservationFees, etc.
     */
    Map<String, Object> getPaymentSummary(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get payments grouped by date period
     * groupBy: "day", "week", "month"
     * Returns: List of {period, totalRevenue, parkingRevenue}
     */
    List<Map<String, Object>> getPaymentsGroupedByPeriod(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String groupBy
    );

    /**
     * Get payments with session filtering (for specific sessions)
     */
    List<VirtualPayment> findBySessionIdsAndDateRange(
            List<Long> sessionIds,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

}
