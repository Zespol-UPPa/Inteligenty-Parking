package com.smartparking.company_service.repo;

import com.smartparking.company_service.model.Company;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository {
    Optional<Company> findById(Long id);
    Optional<Company> findByTaxId(String taxId);
    List<Company> findAll();
    Company save(Company company);
}
