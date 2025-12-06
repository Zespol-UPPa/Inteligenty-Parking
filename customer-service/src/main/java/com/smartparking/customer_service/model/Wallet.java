package com.smartparking.customer_service.model;
import java.math.BigDecimal;
public class Wallet {
    private Long id;
    private BigDecimal balanceMinor;
    private String currencyCode;
    private Long customerId;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public BigDecimal getBalanceMinor() {return balanceMinor;}
    public void setBalanceMinor(BigDecimal balanceMinor) {this.balanceMinor = balanceMinor;}
    public String getCurrencyCode() {return currencyCode;}
    public void setCurrencyCode(String currencyCode) {this.currencyCode = currencyCode;}
    public Long getCustomerId() {return customerId;}
    public void setCustomerId(Long customerId) {this.customerId = customerId;}
}
