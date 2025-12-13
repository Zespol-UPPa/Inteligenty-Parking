package com.smartparking.payment_service.repository;
import com.smartparking.payment_service.model.VirtualPayment;

import java.time.LocalDateTime;
import java.util.List;
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

}
