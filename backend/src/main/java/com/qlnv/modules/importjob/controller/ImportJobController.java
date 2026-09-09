package com.qlnv.modules.importjob.controller;

import com.qlnv.modules.importjob.entity.ImportJob;
import com.qlnv.modules.importjob.service.ImportJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import-jobs")
@RequiredArgsConstructor
@Slf4j
public class ImportJobController {

    private final ImportJobService importJobService;

    /**
     * Upload file và tạo job bất đồng bộ.
     * POST /api/import-jobs/submit?userType=intern
     * → Trả về { jobId, status } ngay lập tức
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userType", defaultValue = "intern") String userType,
            Authentication auth) {
        try {
            String createdBy = auth != null ? auth.getName() : "anonymous";
            String fileName = file.getOriginalFilename();

            // Đọc bytes ngay (stream sẽ đóng sau request)
            byte[] fileBytes = file.getBytes();

            // Tạo job record với PENDING
            ImportJob job = importJobService.createJob(userType, fileName, createdBy);

            // Kích hoạt worker bất đồng bộ (không block)
            importJobService.processImportAsync(job.getId(), fileBytes, fileName, userType);

            log.info("Import job #{} created by {} for userType={}", job.getId(), createdBy, userType);

            return ResponseEntity.ok(Map.of(
                    "jobId",    job.getId(),
                    "status",   job.getStatus(),
                    "fileName", fileName != null ? fileName : "",
                    "message",  "Job đã được tạo, đang xử lý trong nền"
            ));
        } catch (Exception e) {
            log.error("Failed to submit import job", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể tạo import job: " + e.getMessage()));
        }
    }

    /**
     * Submit Google Sheets link và tạo job bất đồng bộ.
     * POST /api/import-jobs/submit-link?userType=intern
     * Body: { "url": "https://docs.google.com/spreadsheets/..." }
     */
    @PostMapping("/submit-link")
    public ResponseEntity<Map<String, Object>> submitImportLink(
            @RequestBody Map<String, String> body,
            @RequestParam(value = "userType", defaultValue = "intern") String userType,
            Authentication auth) {
        try {
            String url = body != null ? body.get("url") : null;
            if (url == null || url.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "URL Google Sheets không được để trống"));
            }
            String createdBy = auth != null ? auth.getName() : "anonymous";
            ImportJob job = importJobService.createJob(userType, url.trim(), createdBy);

            importJobService.processImportUrlAsync(job.getId(), url.trim(), userType);

            log.info("Import link job #{} created by {} for userType={}", job.getId(), createdBy, userType);

            return ResponseEntity.ok(Map.of(
                    "jobId",    job.getId(),
                    "status",   job.getStatus(),
                    "fileName", "Google Sheets",
                    "message",  "Job đã được tạo, đang xử lý trong nền"
            ));
        } catch (Exception e) {
            log.error("Failed to submit link import job", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Không thể tạo import job: " + e.getMessage()));
        }
    }

    /**
     * Polling trạng thái job.
     * GET /api/import-jobs/{jobId}
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<ImportJob> getJobStatus(@PathVariable Integer jobId) {
        try {
            return ResponseEntity.ok(importJobService.getJob(jobId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lấy 20 job gần nhất.
     * GET /api/import-jobs
     */
    @GetMapping
    public ResponseEntity<List<ImportJob>> getRecentJobs() {
        return ResponseEntity.ok(importJobService.getRecentJobs());
    }
}
