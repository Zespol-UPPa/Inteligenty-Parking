package com.smartparking.customer_service.repository;
import com.smartparking.customer_service.model.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findById(Long id);
    Optional<Customer> findByAccountId(Long refAccountId);
    List<Customer> findAll();
    Customer save(Customer customer);
}
