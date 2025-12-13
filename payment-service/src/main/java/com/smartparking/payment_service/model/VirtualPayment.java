package com.smartparking.payment_service.model;

import java.time.LocalDateTime;
public class VirtualPayment {
    private Long id;
    private Integer amountMinor;
    private String currencyCode;
    private String statusPaid;
    private LocalDateTime dateTransaction;
    private Long refAccountId;
    private Long refSessionId;
    private String activity;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public Integer getAmountMinor() {return amountMinor;}
    public void setAmountMinor(Integer amountMinor) {this.amountMinor = amountMinor;}
    public String getCurrencyCode() {return currencyCode;}
    public void setCurrencyCode(String currencyCode) {this.currencyCode = currencyCode;}
    public String getStatusPaid() {return statusPaid;}
    public void setStatusPaid(String statusPaid) {this.statusPaid = statusPaid;}
    public LocalDateTime getDateTransaction() {return dateTransaction;}
    public void setDateTransaction(LocalDateTime dateTransaction) {this.dateTransaction = dateTransaction;}
    public Long getRefAccountId() {return refAccountId;}
    public void setRefAccountId(Long refAccountId) {this.refAccountId = refAccountId;}
    public Long getRefSessionId() {return refSessionId;}
    public void setRefSessionId(Long refSessionId) {this.refSessionId = refSessionId;}
    public String getActivity() {return activity;}
    public void setActivity(String activity) {this.activity = activity;}
}
