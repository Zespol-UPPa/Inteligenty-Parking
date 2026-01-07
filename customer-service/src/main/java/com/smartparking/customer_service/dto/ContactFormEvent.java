package com.smartparking.customer_service.dto;

public class ContactFormEvent {
    private String userEmail;
    private String userName;
    private String subject;
    private String message;
    private Long accountId;

    public ContactFormEvent() {}

    public ContactFormEvent(String userEmail, String userName, String subject, String message, Long accountId) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.subject = subject;
        this.message = message;
        this.accountId = accountId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
}

