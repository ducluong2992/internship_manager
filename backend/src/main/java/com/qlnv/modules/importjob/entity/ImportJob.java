package com.qlnv.modules.importjob.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_jobs", indexes = {
    @Index(name = "idx_import_jobs_status", columnList = "status"),
    @Index(name = "idx_import_jobs_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** PENDING / PROCESSING / SUCCESS / FAILED */
    @Builder.Default
    @Column(nullable = false)
    private String status = "PENDING";

    /** intern / employee */
    @Column(name = "job_type", nullable = false)
    private String jobType;

    /** Tên file gốc */
    @Column(name = "file_name")
    private String fileName;

    /** Người tạo job */
    @Column(name = "created_by")
    private String createdBy;

    /** Tiến độ: VD "45/200" */
    private String progress;

    /** 0-100 */
    @Builder.Default
    @Column(name = "percent")
    private Integer percent = 0;

    /** JSON kết quả cuối: { imported, updated, skipped, errors } */
    @Column(columnDefinition = "TEXT")
    private String result;

    /** Thông báo lỗi nếu FAILED */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
