package com.qlnv.modules.document.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tài liệu tri thức được upload lên hệ thống.
 * Sau khi upload, pipeline RAG sẽ chunk và embed nội dung (xem DocumentIndexingService).
 */

@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_documents_status_active", columnList = "status, is_active"),
    @Index(name = "idx_documents_uploaded_by",   columnList = "uploaded_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String filename;

    @Builder.Default
    @Column(nullable = false)
    private String status = "PROCESSING"; // PROCESSING / READY / ERROR

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    /**
     * Trạng thái RAG indexing: PENDING → INDEXING → INDEXED | ERROR
     * PENDING: chưa index (mặc định khi upload)
     * INDEXING: đang chunk + embed
     * INDEXED: đã hoàn thành, sẵn sàng cho RAG search
     * ERROR: lỗi trong quá trình indexing
     */
    @Builder.Default
    @Column(name = "index_status")
    private String indexStatus = "PENDING";

    /** Số lượng chunk đã tạo ra từ tài liệu này */
    @Column(name = "chunk_count")
    private Integer chunkCount;

    /** Thời điểm hoàn thành indexing lần cuối */
    @Column(name = "indexed_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime indexedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
