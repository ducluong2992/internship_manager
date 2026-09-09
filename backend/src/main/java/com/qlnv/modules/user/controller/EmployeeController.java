package com.qlnv.modules.user.controller;

import com.qlnv.modules.importjob.entity.ImportJob;
import com.qlnv.modules.importjob.service.ImportJobService;
import com.qlnv.modules.user.dto.ImportResultDto;
import com.qlnv.modules.user.dto.ManagerResponse;
import com.qlnv.modules.user.dto.PositionResponse;
import com.qlnv.modules.user.dto.UserRequestDto;
import com.qlnv.modules.user.dto.UserResponse;
import com.qlnv.modules.user.service.PositionService;
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
@RequestMapping({"/employees", "/employees/"})
@RequiredArgsConstructor
public class EmployeeController {

    private final UserService userService;
    private final PositionService positionService;
    private final UserExcelService userExcelService;
    private final ImportJobService importJobService;

    @GetMapping("/positions")
    public ResponseEntity<List<PositionResponse>> getPositions() {
        return ResponseEntity.ok(positionService.getAllPositions());
    }

    @GetMapping("/managers")
    public ResponseEntity<List<ManagerResponse>> getManagers() {
        return ResponseEntity.ok(userService.getManagers());
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<UserResponse>> getEmployees(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(required = false, name = "working_status") String workingStatus,
            @RequestParam(required = false, name = "staff_category") String staffCategory,
            @RequestParam(required = false, name = "employment_status") String employmentStatus,
            @RequestParam(required = false, name = "join_date_from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDateFrom,
            @RequestParam(required = false, name = "join_date_to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDateTo,
            @RequestParam(required = false, name = "borrow_end_from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate borrowEndFrom,
            @RequestParam(required = false, name = "borrow_end_to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate borrowEndTo) {

        return ResponseEntity.ok(userService.queryUsers(
                keyword, "employee", null, project, workingStatus, staffCategory, employmentStatus,
                joinDateFrom, joinDateTo, borrowEndFrom, borrowEndTo
        ));
    }

    @PostMapping({"", "/"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createEmployee(@Valid @RequestBody UserRequestDto req) {
        return ResponseEntity.ok(userService.createUser(req, "employee"));
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateEmployee(
            @PathVariable("employeeId") Integer employeeId,
            @Valid @RequestBody UserRequestDto req) {
        return ResponseEntity.ok(userService.updateUser(employeeId, req));
    }

    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteEmployee(@PathVariable("employeeId") Integer employeeId) {
        userService.deleteUser(employeeId);
        return ResponseEntity.ok(Map.of("message", "Xóa thành công"));
    }

    @DeleteMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteAllEmployees() {
        return ResponseEntity.ok(userService.deleteAllByUserType("employee"));
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> getImportTemplate() {
        byte[] data = userExcelService.generateEmployeeImportTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Template_Nhan_Su.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.qlnv.modules.user.dto.ReconcilePreviewDto> importEmployees(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userExcelService.previewEmployeeImportFile(file));
    }

    @PostMapping(value = "/preview-import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.qlnv.modules.user.dto.ReconcilePreviewDto> previewImportFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userExcelService.previewEmployeeImportFile(file));
    }

    @PostMapping("/preview-import-link")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.qlnv.modules.user.dto.ReconcilePreviewDto> previewImportLink(@RequestBody com.qlnv.modules.user.dto.LinkImportRequest req) {
        return ResponseEntity.ok(userExcelService.previewEmployeeImportLink(req.getUrl()));
    }

    @PostMapping("/confirm-import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> confirmImport(@RequestBody com.qlnv.modules.user.dto.ConfirmImportRequest req) {
        return ResponseEntity.ok(userExcelService.confirmEmployeeImport(req));
    }

    @PostMapping("/confirm-import-link")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> confirmImportLink(@RequestBody com.qlnv.modules.user.dto.ConfirmImportRequest req) {
        return ResponseEntity.ok(userExcelService.confirmEmployeeImport(req));
    }

    /**
     * Async confirm cho nhan vien — tra ve jobId ngay, xu ly nen.
     * POST /employees/confirm-import-async
     */
    @PostMapping("/confirm-import-async")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> confirmImportAsync(
            @RequestBody com.qlnv.modules.user.dto.ConfirmImportRequest req,
            Authentication auth) {
        String createdBy = auth != null ? auth.getName() : "admin";
        ImportJob job = importJobService.createJob("employee", "reconcile-confirm", createdBy);
        importJobService.processConfirmAsync(job.getId(), req, "employee");
        return ResponseEntity.ok(Map.of(
                "jobId",   job.getId(),
                "status",  "PENDING",
                "message", "Dang xu ly " + (req.getAdditions() != null ? req.getAdditions().size() : 0)
                           + " them moi, " + (req.getUpdates() != null ? req.getUpdates().size() : 0) + " cap nhat"
        ));
    }

    @PostMapping("/import-link")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.qlnv.modules.user.dto.ReconcilePreviewDto> importEmployeesLink(@RequestBody com.qlnv.modules.user.dto.LinkImportRequest req) {
        return ResponseEntity.ok(userExcelService.previewEmployeeImportLink(req.getUrl()));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEmployees(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(required = false, name = "working_status") String workingStatus,
            @RequestParam(required = false, name = "staff_category") String staffCategory,
            @RequestParam(required = false, name = "employment_status") String employmentStatus) {

        List<UserResponse> list = userService.queryUsers(
                keyword, "employee", null, project, workingStatus, staffCategory, employmentStatus,
                null, null, null, null
        );
        byte[] data = userExcelService.exportEmployeesToExcel(list);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Danh_sach_nhan_su.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
