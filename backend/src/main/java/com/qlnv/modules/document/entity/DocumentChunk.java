package com.qlnv.modules.document.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Một đoạn văn bản (chunk) được tách ra từ tài liệu gốc.
 * Mỗi chunk có embedding vector (JSON array float[768]) để phục vụ RAG semantic search.
 */
@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_chunks_document_id", columnList = "document_id"),
    @Index(name = "idx_chunks_doc_active",  columnList = "document_id, chunk_index")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK tới bảng documents */
    @Column(name = "document_id", nullable = false)
    private Integer documentId;

    /** Thứ tự chunk trong tài liệu (0-based) */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    /** Tiêu đề section chứa chunk này (null nếu không detect được) */
    @Column(name = "section_title")
    private String sectionTitle;

    /** Số trang (ước tính, null nếu không xác định được) */
    @Column(name = "page_number")
    private Integer pageNumber;

    /** Nội dung văn bản của chunk */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Độ dài nội dung tính bằng ký tự */
    @Column(name = "content_length")
    private Integer contentLength;

    /**
     * Embedding vector dưới dạng JSON array: "[0.12, -0.34, ...]"
     * Dimension: 768 (text-embedding-004)
     */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    /** Tên model đã dùng để tạo embedding */
    @Column(name = "embedding_model")
    private String embeddingModel;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
