package com.smartparking.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "routes")
public class RoutesProperties {
    // Spring Boot automatically maps kebab-case (customer-service) to camelCase (customerService)
    private String customerService;
    private String paymentService;
    private String parkingService;
    private String adminService;
    private String workerService;
    private String accountsService;
    private String companyService;
    private String ocrService;

    public String getCustomerService() { return customerService; }
    public void setCustomerService(String customerService) { this.customerService = customerService; }
    public String getPaymentService() { return paymentService; }
    public void setPaymentService(String paymentService) { this.paymentService = paymentService; }
    public String getParkingService() { return parkingService; }
    public void setParkingService(String parkingService) { this.parkingService = parkingService; }
    public String getAdminService() { return adminService; }
    public void setAdminService(String adminService) { this.adminService = adminService; }
    public String getWorkerService() { return workerService; }
    public void setWorkerService(String workerService) { this.workerService = workerService; }
    public String getAccountsService() { return accountsService; }
    public void setAccountsService(String accountsService) { this.accountsService = accountsService; }
    public String getCompanyService() { return companyService; }
    public void setCompanyService(String companyService) { this.companyService = companyService; }
    public String getOcrService() { return ocrService; }
    public void setOcrService(String ocrService) { this.ocrService = ocrService; }
}
