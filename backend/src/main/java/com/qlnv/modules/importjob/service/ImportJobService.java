package com.qlnv.modules.importjob.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlnv.common.config.CustomByteArrayMultipartFile;
import com.qlnv.modules.importjob.entity.ImportJob;
import com.qlnv.modules.importjob.repository.ImportJobRepository;
import com.qlnv.modules.user.dto.ConfirmImportRequest;
import com.qlnv.modules.user.dto.ImportResultDto;
import com.qlnv.modules.user.service.UserExcelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportJobService {

    private final ImportJobRepository importJobRepository;
    private final UserExcelService userExcelService;
    private final ObjectMapper objectMapper;

    /**
     * Tạo job mới, lưu DB với PENDING và trả về ngay.
     */
    public ImportJob createJob(String jobType, String fileName, String createdBy) {
        ImportJob job = ImportJob.builder()
                .jobType(jobType)
                .fileName(fileName)
                .createdBy(createdBy)
                .status("PENDING")
                .percent(0)
                .build();
        return importJobRepository.save(job);
    }

    /**
     * Lấy thông tin job theo ID.
     */
    public ImportJob getJob(Integer jobId) {
        return importJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job không tồn tại: " + jobId));
    }

    /**
     * Lấy 20 job gần nhất.
     */
    public List<ImportJob> getRecentJobs() {
        return importJobRepository.findTop20ByOrderByCreatedAtDesc();
    }

    // ─────────────────────────────────────────────────────────
    // Worker 1: Import file Excel thô bất đồng bộ
    // ─────────────────────────────────────────────────────────

    /**
     * Worker bất đồng bộ — chạy trên thread riêng.
     * Nhận bytes của file để tránh stream bị đóng trước khi xử lý.
     */
    @Async("importTaskExecutor")
    public void processImportAsync(Integer jobId, byte[] fileBytes, String originalFilename, String userType) {
        ImportJob job = importJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("ImportJob not found: {}", jobId);
            return;
        }

        job.setStatus("PROCESSING");
        job.setProgress("0/?");
        job.setPercent(5);
        importJobRepository.save(job);

        try {
            MultipartFile mockFile = new CustomByteArrayMultipartFile(originalFilename, fileBytes);
            ImportResultDto result = userExcelService.importUsersFromExcel(mockFile, userType);

            job.setStatus("SUCCESS");
            job.setPercent(100);
            job.setProgress(result.getImportedCount() + " mới, " + result.getUpdatedCount() + " cập nhật");
            job.setResult(objectMapper.writeValueAsString(Map.of(
                    "importedCount", result.getImportedCount(),
                    "updatedCount",  result.getUpdatedCount(),
                    "skippedCount",  result.getSkippedCount(),
                    "errors",        result.getErrors()
            )));
            log.info("ImportJob #{} SUCCESS — imported={}, updated={}", jobId,
                    result.getImportedCount(), result.getUpdatedCount());

        } catch (Exception e) {
            log.error("ImportJob #{} FAILED", jobId, e);
            job.setStatus("FAILED");
            job.setPercent(0);
            job.setErrorMessage(e.getMessage());
        } finally {
            importJobRepository.save(job);
        }
    }

    /**
     * Worker bất đồng bộ — tải dữ liệu từ Google Sheets link và import trực tiếp.
     */
    @Async("importTaskExecutor")
    public void processImportUrlAsync(Integer jobId, String url, String userType) {
        ImportJob job = importJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("ImportJob not found: {}", jobId);
            return;
        }

        job.setStatus("PROCESSING");
        job.setProgress("Đang tải dữ liệu từ link...");
        job.setPercent(5);
        importJobRepository.save(job);

        try {
            ImportResultDto result = userExcelService.importUsersFromUrl(url, userType);

            job.setStatus("SUCCESS");
            job.setPercent(100);
            job.setProgress(result.getImportedCount() + " mới, " + result.getUpdatedCount() + " cập nhật");
            job.setResult(objectMapper.writeValueAsString(Map.of(
                    "importedCount", result.getImportedCount(),
                    "updatedCount",  result.getUpdatedCount(),
                    "skippedCount",  result.getSkippedCount(),
                    "errors",        result.getErrors()
            )));
            log.info("ImportJob URL #{} SUCCESS — imported={}, updated={}", jobId,
                    result.getImportedCount(), result.getUpdatedCount());

        } catch (Exception e) {
            log.error("ImportJob URL #{} FAILED", jobId, e);
            job.setStatus("FAILED");
            job.setPercent(0);
            job.setErrorMessage(e.getMessage());
        } finally {
            importJobRepository.save(job);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Worker 2: Confirm reconcile bất đồng bộ
    // ─────────────────────────────────────────────────────────

    /**
     * Worker bất đồng bộ cho bước confirm reconcile.
     * Tái dùng confirmInternImportLink / confirmEmployeeImport có sẵn trong UserExcelService.
     */
    @Async("importTaskExecutor")
    public void processConfirmAsync(Integer jobId, ConfirmImportRequest req, String userType) {
        ImportJob job = importJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("ConfirmJob not found: {}", jobId);
            return;
        }

        int totalOps = (req.getAdditions() != null ? req.getAdditions().size() : 0)
                + (req.getUpdates() != null ? req.getUpdates().size() : 0)
                + (req.getDeleteIds() != null ? req.getDeleteIds().size() : 0);

        job.setStatus("PROCESSING");
        job.setProgress("0/" + totalOps);
        job.setPercent(5);
        importJobRepository.save(job);

        try {
            Map<String, Object> result;
            if ("employee".equalsIgnoreCase(userType)) {
                result = userExcelService.confirmEmployeeImport(req);
            } else {
                result = userExcelService.confirmInternImportLink(req);
            }

            int added   = toInt(result.get("addedCount"));
            int updated = toInt(result.get("updatedCount"));
            int deleted = toInt(result.get("deletedCount"));

            job.setStatus("SUCCESS");
            job.setPercent(100);
            job.setProgress(added + " thêm, " + updated + " cập nhật, " + deleted + " xóa");
            job.setResult(objectMapper.writeValueAsString(result));
            log.info("ConfirmJob #{} SUCCESS — added={}, updated={}, deleted={}", jobId, added, updated, deleted);

        } catch (Exception e) {
            log.error("ConfirmJob #{} FAILED", jobId, e);
            job.setStatus("FAILED");
            job.setPercent(0);
            job.setErrorMessage(e.getMessage());
        } finally {
            importJobRepository.save(job);
        }
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }
}
