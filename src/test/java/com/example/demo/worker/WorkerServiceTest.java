package com.example.demo.worker;

import com.example.demo.attendance.AttendanceService;
import com.example.demo.common.Designation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private AttendanceService attendanceService;

    @InjectMocks
    private WorkerServiceImpl workerService;

    @Test
    void createWorker_Success() {
        Worker worker = Worker.builder().name("John").build();
        when(workerRepository.save(worker)).thenReturn(worker);

        Worker result = workerService.createWorker(worker);

        assertEquals("John", result.getName());
        verify(workerRepository).save(worker);
    }

    @Test
    void updateWorker_Success_InvalidatesCache() {
        Worker existing = Worker.builder().id(1L).name("Old").build();
        Worker update = Worker.builder().name("New").designation(Designation.MASON).dailyWageRate(new BigDecimal("100")).activeStatus(true).build();

        when(workerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(workerRepository.save(any(Worker.class))).thenReturn(update);

        Worker result = workerService.updateWorker(1L, update);

        assertEquals("New", result.getName());
        verify(attendanceService).invalidateWorkerCache(1L);
        verify(workerRepository).save(any(Worker.class));
    }

    @Test
    void updateWorker_NotFound_ThrowsException() {
        when(workerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workerService.updateWorker(1L, new Worker()));
    }

    @Test
    void getAllWorkers_ReturnsList() {
        when(workerRepository.findAll()).thenReturn(List.of(new Worker(), new Worker()));

        List<Worker> result = workerService.getAllWorkers();

        assertEquals(2, result.size());
        verify(workerRepository).findAll();
    }
}
