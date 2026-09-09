package com.qlnv.modules.user.controller;

import com.qlnv.modules.importjob.entity.ImportJob;
import com.qlnv.modules.importjob.service.ImportJobService;
import com.qlnv.modules.user.dto.AdminAccountRow;
import com.qlnv.modules.user.dto.ConfirmImportRequest;
import com.qlnv.modules.user.dto.ImportResultDto;
import com.qlnv.modules.user.dto.LinkImportRequest;
import com.qlnv.modules.user.dto.ReconcilePreviewDto;
import com.qlnv.modules.user.dto.UserRequestDto;
import com.qlnv.modules.user.dto.UserResponse;
import com.qlnv.modules.user.service.UserExcelService;
import com.qlnv.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final UserExcelService userExcelService;
    private final ImportJobService importJobService;

    // ─── Users (Interns / All) Management ───

    @GetMapping({"/users", "/users/"})
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(required = false, name = "user_type") String userType,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(required = false, name = "working_status") String workingStatus,
            @RequestParam(required = false, name = "staff_category") String staffCategory,
            @RequestParam(required = false, name = "employment_status") String employmentStatus,
            @RequestParam(required = false, name = "join_date_from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDateFrom,
            @RequestParam(required = false, name = "join_date_to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDateTo,
            @RequestParam(required = false, name = "borrow_end_from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate borrowEndFrom,
            @RequestParam(required = false, name = "borrow_end_to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate borrowEndTo) {

        return ResponseEntity.ok(userService.queryUsers(
                keyword, userType, role, project, workingStatus, staffCategory, employmentStatus,
                joinDateFrom, joinDateTo, borrowEndFrom, borrowEndTo
        ));
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequestDto req) {
        return ResponseEntity.ok(userService.createUser(req, "intern"));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("userId") Integer userId,
            @Valid @RequestBody UserRequestDto req) {
        return ResponseEntity.ok(userService.updateUser(userId, req));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable("userId") Integer userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "Xóa thành công"));
    }

    @DeleteMapping("/users/all")
    public ResponseEntity<Map<String, Object>> deleteAllUsers() {
        return ResponseEntity.ok(userService.deleteAllByUserType("intern"));
    }

    @GetMapping("/users/import-template")
    public ResponseEntity<byte[]> getImportTemplate() {
        byte[] data = userExcelService.generateInternImportTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Template_TTS.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReconcilePreviewDto> importUsers(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userExcelService.previewInternImportFile(file));
    }

    @PostMapping(value = "/users/preview-import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReconcilePreviewDto> previewImportFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userExcelService.previewInternImportFile(file));
    }

    @PostMapping("/users/preview-import-link")
    public ResponseEntity<ReconcilePreviewDto> previewImportLink(@RequestBody LinkImportRequest req) {
        return ResponseEntity.ok(userExcelService.previewInternImportLink(req.getUrl()));
    }

    @PostMapping("/users/confirm-import-link")
    public ResponseEntity<Map<String, Object>> confirmImportLink(@RequestBody ConfirmImportRequest req) {
        return ResponseEntity.ok(userExcelService.confirmInternImportLink(req));
    }

    @PostMapping("/users/confirm-import")
    public ResponseEntity<Map<String, Object>> confirmImport(@RequestBody ConfirmImportRequest req) {
        return ResponseEntity.ok(userExcelService.confirmInternImportLink(req));
    }

    /**
     * Async confirm — trả về jobId ngay, xử lý nền.
     * POST /admin/users/confirm-import-async
     */
    @PostMapping("/users/confirm-import-async")
    public ResponseEntity<Map<String, Object>> confirmImportAsync(
            @RequestBody ConfirmImportRequest req,
            Authentication auth) {
        String createdBy = auth != null ? auth.getName() : "admin";
        ImportJob job = importJobService.createJob("intern", "reconcile-confirm", createdBy);
        importJobService.processConfirmAsync(job.getId(), req, "intern");
        return ResponseEntity.ok(Map.of(
                "jobId",   job.getId(),
                "status",  "PENDING",
                "message", "Đang xử lý " + (req.getAdditions() != null ? req.getAdditions().size() : 0)
                           + " thêm mới, " + (req.getUpdates() != null ? req.getUpdates().size() : 0) + " cập nhật"
        ));
    }

    @PostMapping("/users/import-link")
    public ResponseEntity<ReconcilePreviewDto> importLink(@RequestBody LinkImportRequest req) {
        return ResponseEntity.ok(userExcelService.previewInternImportLink(req.getUrl()));
    }

    @PatchMapping("/users/{userId}/lock")
    public ResponseEntity<Map<String, Object>> lockUser(
            @PathVariable("userId") Integer userId,
            @RequestBody(required = false) Map<String, Integer> body) {
        Integer status = (body != null && body.containsKey("status")) ? body.get("status") : null;
        return ResponseEntity.ok(userService.lockUser(userId, status));
    }

    @PostMapping("/users/lock-resigned-accounts")
    public ResponseEntity<Map<String, Object>> lockResignedAccounts() {
        return ResponseEntity.ok(userService.lockResignedAccounts());
    }

    @PatchMapping("/users/{userId}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable("userId") Integer userId,
            @RequestBody(required = false) Map<String, String> body) {
        String newPwd = (body != null) ? body.get("new_password") : "123456";
        return ResponseEntity.ok(userService.resetPassword(userId, newPwd));
    }

    // ─── Accounts Management ───

    @GetMapping("/accounts")
    public ResponseEntity<List<AdminAccountRow>> getAccounts() {
        return ResponseEntity.ok(userService.getAccountsList());
    }

    @GetMapping("/accounts/export")
    public ResponseEntity<byte[]> exportAccounts() {
        byte[] data = userExcelService.exportAccountsToExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Danh_sach_tai_khoan.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    // ─── Stats ───

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(userService.getStats());
    }
}
