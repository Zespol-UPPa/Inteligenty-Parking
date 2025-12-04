package com.smartparking.admin_service.dto;

public class IdResponse {
    private long id;

    public IdResponse() { }

    public IdResponse(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}