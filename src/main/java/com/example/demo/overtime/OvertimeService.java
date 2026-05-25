package com.example.demo.overtime;

import com.example.demo.attendance.AttendanceLog;

import java.math.BigDecimal;
import java.util.List;

public interface OvertimeService {
    void calculateAndSaveOvertime(AttendanceLog log);
    List<OvertimeEntry> getOvertimeSummary(Long workerId, String month);
    BigDecimal settleOvertime(Long workerId, String month);
}
