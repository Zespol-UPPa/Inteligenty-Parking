package com.smartparking.core.dto;

import com.smartparking.core.interfaces.Identifiable;
import java.math.BigDecimal;

public final class WalletDto implements Identifiable {
    private final Long id;
    private final BigDecimal balanceMinor;
    private final String currencyCode;
    private final Long accountId;

    public WalletDto(Long id, BigDecimal balanceMinor, String currencyCode, Long accountId) {
        this.id = id;
        this.balanceMinor = balanceMinor;
        this.currencyCode = currencyCode;
        this.accountId = accountId;
    }

    @Override
    public Long getId() { return id; }
    public BigDecimal getBalanceMinor() { return balanceMinor; }
    public String getCurrencyCode() { return currencyCode; }
    public Long getAccountId() { return accountId; }
}
