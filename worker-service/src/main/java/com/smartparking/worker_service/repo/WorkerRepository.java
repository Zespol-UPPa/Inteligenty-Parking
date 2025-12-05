package com.smartparking.worker_service.repo;

import com.smartparking.worker_service.model.Worker;
import java.util.List;
import java.util.Optional;
public interface WorkerRepository {
    Optional<Worker> findById(Long id);
    Optional<Worker> findByAccountId(Long accountId);
    List<Worker> findAll();
    Worker save(Worker admin);
}
