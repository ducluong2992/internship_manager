package com.qlnv.modules.document.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
