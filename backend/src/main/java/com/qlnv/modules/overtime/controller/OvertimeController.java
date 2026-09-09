package com.qlnv.modules.overtime.controller;

import com.qlnv.common.security.UserPrincipal;
import com.qlnv.modules.overtime.dto.OvertimeRequestDto;
import com.qlnv.modules.overtime.dto.OvertimeResponseDto;
import com.qlnv.modules.overtime.service.OvertimeCalculator;
import com.qlnv.modules.overtime.service.OvertimeExportService;
import com.qlnv.modules.overtime.service.OvertimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/overtime", "/overtime/"})
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;
    private final OvertimeExportService overtimeExportService;

    @PostMapping({"", "/"})
    public ResponseEntity<List<OvertimeResponseDto.Response>> createOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OvertimeRequestDto req) {
        return ResponseEntity.ok(overtimeService.createOvertime(principal.getId(), req));
    }

    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewOvertime(
            @Valid @RequestBody OvertimeRequestDto req) {
        List<OvertimeCalculator.Segment> segments = overtimeService.preview(req);
        double totalRaw = segments.stream().mapToDouble(s -> s.getRawHours() != null ? s.getRawHours() : 0.0).sum();
        double totalWeighted = segments.stream().mapToDouble(s -> s.getWeightedHours() != null ? s.getWeightedHours() : 0.0).sum();
        return ResponseEntity.ok(Map.of(
                "segments", segments,
                "total_raw_hours", Math.round(totalRaw * 100.0) / 100.0,
                "total_weighted_hours", Math.round(totalWeighted * 100.0) / 100.0
        ));
    }

    @GetMapping("/my")
    public ResponseEntity<List<OvertimeResponseDto.Response>> getMyOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, name = "from_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false, name = "to_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(overtimeService.getMyOvertime(principal.getId(), fromDate, toDate, status));
    }

    @GetMapping("/my/stats")
    public ResponseEntity<Map<String, Object>> getMyStats(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        return ResponseEntity.ok(overtimeService.getMyStats(principal.getId(), month, year));
    }

    @PutMapping("/{otId}")
    public ResponseEntity<OvertimeResponseDto.Response> updateOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("otId") Integer otId,
            @Valid @RequestBody OvertimeRequestDto req) {
        boolean isAdmin = "admin".equalsIgnoreCase(principal.getRole());
        return ResponseEntity.ok(overtimeService.updateOvertime(principal.getId(), otId, req, isAdmin));
    }

    @DeleteMapping("/{otId}")
    public ResponseEntity<Map<String, String>> deleteOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("otId") Integer otId) {
        boolean isAdmin = "admin".equalsIgnoreCase(principal.getRole());
        overtimeService.deleteOvertime(principal.getId(), otId, isAdmin);
        return ResponseEntity.ok(Map.of("message", "Xóa đơn OT thành công"));
    }

    // ─── Admin Endpoints ───

    @DeleteMapping("/admin/{otId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminDeleteOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("otId") Integer otId) {
        overtimeService.deleteOvertime(principal.getId(), otId, true);
        return ResponseEntity.ok(Map.of("message", "Admin xóa đơn OT thành công"));
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OvertimeResponseDto.Response>> getAdminList(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(required = false, name = "from_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false, name = "to_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(required = false, name = "employee_name") String employeeName) {
        String effectiveKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword : employeeName;
        return ResponseEntity.ok(overtimeService.getAdminOvertimeList(status, project, month, year, fromDate, toDate, effectiveKeyword));
    }

    @GetMapping("/admin/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminSummary(
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(required = false, name = "employee_name") String employeeName) {
        String effectiveKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword : employeeName;
        return ResponseEntity.ok(overtimeService.getAdminOvertimeSummary(month, year, project, effectiveKeyword));
    }

    @PostMapping("/admin/{otId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OvertimeResponseDto.Response> approveOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("otId") Integer otId) {
        return ResponseEntity.ok(overtimeService.approveOvertime(otId, principal.getId()));
    }

    @PostMapping("/admin/{otId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OvertimeResponseDto.Response> rejectOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("otId") Integer otId,
            @RequestBody(required = false) OvertimeResponseDto.Approve req) {
        String reason = req != null ? req.getRejectReason() : null;
        return ResponseEntity.ok(overtimeService.rejectOvertime(otId, principal.getId(), reason));
    }

    @PostMapping("/admin/{otId}/reset-pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OvertimeResponseDto.Response> resetPendingOvertime(@PathVariable("otId") Integer otId) {
        return ResponseEntity.ok(overtimeService.resetPendingOvertime(otId));
    }

    @PostMapping("/admin/approve-selected")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> approveSelected(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody OvertimeResponseDto.ApproveSelected req) {
        return ResponseEntity.ok(overtimeService.approveSelected(req.getIds(), principal.getId()));
    }

    @PostMapping("/admin/approve-all-pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> approveAllPending(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "project", required = false) String project) {
        return ResponseEntity.ok(overtimeService.approveAllPending(principal.getId(), month, year, project));
    }

    @GetMapping("/admin/export-pl02")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportOvertimePL02(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(required = false, name = "from_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false, name = "to_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(required = false, name = "employee_name") String employeeName) {
        String effectiveKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword : employeeName;
        byte[] data = overtimeExportService.exportPhuLuc02Summary(status, project, month, year, fromDate, toDate, effectiveKeyword);
        int targetMonth = (month != null) ? month : (fromDate != null ? fromDate.getMonthValue() : LocalDate.now().getMonthValue());
        int targetYear = (year != null) ? year : (fromDate != null ? fromDate.getYear() : LocalDate.now().getYear());
        String fileName = String.format("Phu_Luc_02_Bang_Tong_Hop_Cong_OT_T%02d_%d.xlsx", targetMonth, targetYear);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/admin/export-pl03")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportOvertimePL03(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(required = false, name = "from_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false, name = "to_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(required = false, name = "employee_name") String employeeName) {
        String effectiveKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword : employeeName;
        byte[] data = overtimeExportService.exportPhuLuc03DetailByProject(status, project, month, year, fromDate, toDate, effectiveKeyword);
        int targetMonth = (month != null) ? month : (fromDate != null ? fromDate.getMonthValue() : LocalDate.now().getMonthValue());
        int targetYear = (year != null) ? year : (fromDate != null ? fromDate.getYear() : LocalDate.now().getYear());
        String fileName = String.format("Phu_Luc_03_Thoi_Gian_Lam_Them_Gio_CBNV_T%02d_%d.xlsx", targetMonth, targetYear);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/admin/export-zip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportOvertimeZip(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(required = false, name = "from_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false, name = "to_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(required = false, name = "employee_name") String employeeName) {
        String effectiveKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword : employeeName;
        byte[] data = overtimeExportService.exportZipBundle(status, project, month, year, fromDate, toDate, effectiveKeyword);
        int targetMonth = (month != null) ? month : (fromDate != null ? fromDate.getMonthValue() : LocalDate.now().getMonthValue());
        int targetYear = (year != null) ? year : (fromDate != null ? fromDate.getYear() : LocalDate.now().getYear());
        String fileName = String.format("Bao_Cao_OT_PL02_PL03_T%02d_%d.zip", targetMonth, targetYear);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(data);
    }

    @GetMapping("/admin/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportOvertime(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(required = false, name = "from_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false, name = "to_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(required = false, name = "employee_name") String employeeName) {
        return exportOvertimePL03(status, project, month, year, fromDate, toDate, keyword, employeeName);
    }
}

