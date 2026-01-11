package com.smartparking.admin_service.repo;

import com.smartparking.admin_service.model.Admin;
import java.util.List;
import java.util.Optional;
public interface AdminRepository {
    Optional<Admin> findById(Long id);
    Optional<Admin> findByAccountId(Long accountId);
    List<Admin> findAll();
    Admin save(Admin admin);
    void updatePersonalData(Long accountId, String firstName, String lastName,
                            String phoneNumber, String peselNumber);
    List<Admin> findByCompanyId(Long companyId);

}
