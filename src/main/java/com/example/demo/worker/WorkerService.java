package com.example.demo.worker;

import java.util.Optional;

public interface WorkerService {
    Worker createWorker(Worker worker);
    Worker updateWorker(Long id, Worker workerDetails);
    Optional<Worker> findById(Long id);
    Optional<Worker> findActiveById(Long id);
    java.util.List<Worker> getAllWorkers();
}
