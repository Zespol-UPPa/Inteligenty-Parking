package com.smartparking.accounts_service.model;

public class LoginCode {
    private Long id;
    private String code;
    private Long idAccount;
    private boolean isUsed;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getCode() {return code;}
    public void setCode(String code) {this.code = code;}
    public Long getAccountId() {return idAccount;}
    public void setAccountId(Long idAccount) {this.idAccount = idAccount;}
    public boolean isUsed() {return isUsed;}
    public void setUsed(boolean used) {isUsed = used;}
}
