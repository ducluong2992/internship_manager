package com.qlnv.modules.schedule.controller;

import com.qlnv.modules.schedule.dto.PeriodRequestDto;
import com.qlnv.modules.schedule.dto.PeriodResponse;
import com.qlnv.modules.schedule.service.PeriodService;
import com.qlnv.modules.schedule.service.ScheduleExcelService;
import com.qlnv.modules.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminScheduleController {

    private final PeriodService periodService;
    private final ScheduleService scheduleService;
    private final ScheduleExcelService scheduleExcelService;

    @GetMapping("/periods")
    public ResponseEntity<List<PeriodResponse>> getPeriods() {
        return ResponseEntity.ok(periodService.getAllPeriods());
    }

    @PostMapping("/periods")
    public ResponseEntity<PeriodResponse> createPeriod(@Valid @RequestBody PeriodRequestDto req) {
        return ResponseEntity.ok(periodService.createPeriod(req));
    }

    @PutMapping("/periods/{periodId}")
    public ResponseEntity<PeriodResponse> updatePeriod(
            @PathVariable Integer periodId,
            @Valid @RequestBody PeriodRequestDto req) {
        return ResponseEntity.ok(periodService.updatePeriod(periodId, req));
    }

    @DeleteMapping("/periods/{periodId}")
    public ResponseEntity<Map<String, String>> deletePeriod(@PathVariable Integer periodId) {
        periodService.deletePeriod(periodId);
        return ResponseEntity.ok(Map.of("message", "Xóa kỳ lịch thành công"));
    }

    @GetMapping("/schedule")
    public ResponseEntity<Map<String, Object>> getScheduleMatrix(
            @RequestParam(required = false, name = "period_id") Integer periodId,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(scheduleService.getAdminScheduleMatrix(periodId, project, position, keyword));
    }

    @GetMapping("/schedule/export")
    public ResponseEntity<byte[]> exportSchedule(
            @RequestParam(required = false, name = "period_id") Integer periodId,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String keyword) {
        byte[] data = scheduleExcelService.exportScheduleMatrix(periodId, project, position, keyword);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bang_tong_hop_lich_lam_viec.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
