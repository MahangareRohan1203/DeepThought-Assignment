package com.example.demo.worker;

import com.example.demo.attendance.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {

    private final WorkerRepository workerRepository;
    private final AttendanceService attendanceService;

    @Override
    @Transactional
    public Worker createWorker(Worker worker) {
        return workerRepository.save(worker);
    }

    @Override
    @Transactional
    public Worker updateWorker(Long id, Worker workerDetails) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Worker not found"));
        
        worker.setName(workerDetails.getName());
        worker.setDesignation(workerDetails.getDesignation());
        worker.setDailyWageRate(workerDetails.getDailyWageRate());
        worker.setActiveStatus(workerDetails.getActiveStatus());
        
        Worker updatedWorker = workerRepository.save(worker);
        attendanceService.invalidateWorkerCache(id);
        return updatedWorker;
    }

    @Override
    public Optional<Worker> findById(Long id) {
        return workerRepository.findById(id);
    }

    @Override
    public Optional<Worker> findActiveById(Long id) {
        return workerRepository.findByIdAndActiveStatusTrue(id);
    }

    @Override
    public java.util.List<Worker> getAllWorkers() {
        return workerRepository.findAll();
    }
}
