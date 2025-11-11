package com.smartparking.user_service.service;

import com.smartparking.user_service.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class UserProfileService {
    private final UserRepository users;
    public UserProfileService(UserRepository users) {
        this.users = users;
    }
    public Optional<Map<String, Object>> getById(Long id) {
        return users.findById(id);
    }
    public boolean updateName(Long id, String firstName, String lastName) {
        return users.updateProfile(id, firstName, lastName) > 0;
    }
}


