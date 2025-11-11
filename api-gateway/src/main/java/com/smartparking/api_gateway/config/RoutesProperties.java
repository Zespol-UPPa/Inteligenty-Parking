package com.smartparking.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "routes")
public class RoutesProperties {
    private String userService;
    private String paymentService;
    private String parkingService;
    private String adminService;
    private String workerService;

    public String getUserService() { return userService; }
    public void setUserService(String userService) { this.userService = userService; }
    public String getPaymentService() { return paymentService; }
    public void setPaymentService(String paymentService) { this.paymentService = paymentService; }
    public String getParkingService() { return parkingService; }
    public void setParkingService(String parkingService) { this.parkingService = parkingService; }
    public String getAdminService() { return adminService; }
    public void setAdminService(String adminService) { this.adminService = adminService; }
    public String getWorkerService() { return workerService; }
    public void setWorkerService(String workerService) { this.workerService = workerService; }
}
