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
@RequestMapping("/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;
    private final OvertimeExportService overtimeExportService;

    @PostMapping
    public ResponseEntity<List<OvertimeResponseDto.Response>> createOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OvertimeRequestDto req) {
        return ResponseEntity.ok(overtimeService.createOvertime(principal.getId(), req));
    }

    @PostMapping("/preview")
    public ResponseEntity<List<OvertimeCalculator.Segment>> previewOvertime(
            @Valid @RequestBody OvertimeRequestDto req) {
        return ResponseEntity.ok(overtimeService.preview(req));
    }

    @GetMapping("/my")
    public ResponseEntity<List<OvertimeResponseDto.Response>> getMyOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, name = "from_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false, name = "to_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(overtimeService.getMyOvertime(principal.getId(), fromDate, toDate, status));
    }

    @GetMapping("/my/stats")
    public ResponseEntity<Map<String, Object>> getMyStats(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(overtimeService.getMyStats(principal.getId(), month, year));
    }

    @PutMapping("/{otId}")
    public ResponseEntity<OvertimeResponseDto.Response> updateOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer otId,
            @Valid @RequestBody OvertimeRequestDto req) {
        boolean isAdmin = "admin".equalsIgnoreCase(principal.getRole());
        return ResponseEntity.ok(overtimeService.updateOvertime(principal.getId(), otId, req, isAdmin));
    }

    @DeleteMapping("/{otId}")
    public ResponseEntity<Map<String, String>> deleteOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer otId) {
        boolean isAdmin = "admin".equalsIgnoreCase(principal.getRole());
        overtimeService.deleteOvertime(principal.getId(), otId, isAdmin);
        return ResponseEntity.ok(Map.of("message", "Xóa đơn OT thành công"));
    }

    // ─── Admin Endpoints ───

    @DeleteMapping("/admin/{otId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminDeleteOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer otId) {
        overtimeService.deleteOvertime(principal.getId(), otId, true);
        return ResponseEntity.ok(Map.of("message", "Admin xóa đơn OT thành công"));
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OvertimeResponseDto.Response>> getAdminList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String project,
            @RequestParam(required = false, name = "from_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false, name = "to_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(overtimeService.getAdminOvertimeList(status, project, fromDate, toDate, keyword));
    }

    @GetMapping("/admin/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminSummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String project) {
        return ResponseEntity.ok(overtimeService.getAdminOvertimeSummary(month, year, project));
    }

    @PostMapping("/admin/{otId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OvertimeResponseDto.Response> approveOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer otId) {
        return ResponseEntity.ok(overtimeService.approveOvertime(otId, principal.getId()));
    }

    @PostMapping("/admin/{otId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OvertimeResponseDto.Response> rejectOvertime(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer otId,
            @RequestBody(required = false) OvertimeResponseDto.Approve req) {
        String reason = req != null ? req.getRejectReason() : null;
        return ResponseEntity.ok(overtimeService.rejectOvertime(otId, principal.getId(), reason));
    }

    @PostMapping("/admin/{otId}/reset-pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OvertimeResponseDto.Response> resetPendingOvertime(@PathVariable Integer otId) {
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
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String project) {
        return ResponseEntity.ok(overtimeService.approveAllPending(principal.getId(), month, year, project));
    }

    @GetMapping("/admin/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportOvertime(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String project,
            @RequestParam(required = false, name = "from_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false, name = "to_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String keyword) {
        byte[] data = overtimeExportService.exportOvertimeReport(status, project, fromDate, toDate, keyword);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Phu_Luc_03_Tong_Hop_OT.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
