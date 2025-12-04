package com.smartparking.customer_service.service;

import com.smartparking.customer_service.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomerProfileService {
    private final CustomerRepository customers;
    public CustomerProfileService(CustomerRepository customers) {
        this.customers = customers;
    }
    public Optional<Map<String, Object>> getById(Long id) {
        return customers.findById(id);
    }
    public boolean updateName(Long id, String firstName, String lastName) {
        return customers.updateProfile(id, firstName, lastName) > 0;
    }
}

