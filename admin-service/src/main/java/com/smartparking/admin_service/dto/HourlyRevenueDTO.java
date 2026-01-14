package com.smartparking.admin_service.dto;

/**
 * DTO for hourly revenue data
 * Used for revenue-over-time chart endpoint
 */
public class HourlyRevenueDTO {

    private String hour;
    private Double revenue;

    public HourlyRevenueDTO() {
    }

    public HourlyRevenueDTO(String hour, Double revenue) {
        this.hour = hour;
        this.revenue = revenue;
    }

    // Getters and Setters

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public Double getRevenue() {
        return revenue;
    }

    public void setRevenue(Double revenue) {
        this.revenue = revenue;
    }

    @Override
    public String toString() {
        return "HourlyRevenueDTO{" +
                "hour='" + hour + '\'' +
                ", revenue=" + revenue +
                '}';
    }
}