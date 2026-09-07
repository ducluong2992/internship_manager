package com.qlnv.modules.user.controller;

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
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String project,
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
            @PathVariable Integer employeeId,
            @Valid @RequestBody UserRequestDto req) {
        return ResponseEntity.ok(userService.updateUser(employeeId, req));
    }

    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteEmployee(@PathVariable Integer employeeId) {
        userService.deleteUser(employeeId);
        return ResponseEntity.ok(Map.of("message", "Xóa thành công"));
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
    public ResponseEntity<ImportResultDto> importEmployees(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userExcelService.importUsersFromExcel(file, "employee"));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEmployees(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String project,
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
