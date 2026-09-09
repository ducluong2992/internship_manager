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
            @PathVariable("periodId") Integer periodId,
            @Valid @RequestBody PeriodRequestDto req) {
        return ResponseEntity.ok(periodService.updatePeriod(periodId, req));
    }

    @DeleteMapping("/periods/{periodId}")
    public ResponseEntity<Map<String, String>> deletePeriod(@PathVariable("periodId") Integer periodId) {
        periodService.deletePeriod(periodId);
        return ResponseEntity.ok(Map.of("message", "Xóa kỳ lịch thành công"));
    }

    @GetMapping("/schedule")
    public ResponseEntity<Map<String, Object>> getScheduleMatrix(
            @RequestParam(required = false, name = "period_id") Integer periodId,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "position", required = false) String position,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(scheduleService.getAdminScheduleMatrix(periodId, month, year, project, position, keyword));
    }

    @GetMapping("/schedule/import-template")
    public ResponseEntity<byte[]> getImportTemplate(@RequestParam(value = "period_id", required = false) Integer periodId) {
        byte[] data = scheduleExcelService.generateScheduleImportTemplate(periodId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mau_import_lich.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping(value = "/schedule/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importSchedule(
            @RequestParam(value = "period_id", required = false) Integer periodId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(scheduleExcelService.importScheduleFromExcel(periodId, file));
    }

    @PostMapping("/schedule/import-link-sheets")
    public ResponseEntity<Map<String, Object>> getScheduleLinkSheets(@RequestBody com.qlnv.modules.user.dto.LinkImportRequest req) {
        List<String> sheets = scheduleExcelService.getScheduleLinkSheets(req.getUrl());
        return ResponseEntity.ok(Map.of("sheets", sheets));
    }

    @PostMapping("/schedule/import-link")
    public ResponseEntity<Map<String, Object>> importScheduleLink(
            @RequestParam(value = "period_id", required = false) Integer periodId,
            @RequestBody com.qlnv.modules.user.dto.LinkImportRequest req) {
        return ResponseEntity.ok(scheduleExcelService.importScheduleFromLink(periodId, req.getUrl(), req.getSheetName()));
    }

    @GetMapping("/schedule/export")
    public ResponseEntity<byte[]> exportSchedule(
            @RequestParam(required = false, name = "period_id") Integer periodId,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "position", required = false) String position,
            @RequestParam(value = "keyword", required = false) String keyword) {
        byte[] data = scheduleExcelService.exportScheduleMatrix(periodId, month, year, project, position, keyword);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bang_tong_hop_lich_lam_viec.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
