package com.qlnv.modules.document.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {
    private Integer id;
    private String title;
    private String filename;
    private String status;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("uploaded_by")
    private String uploadedBy;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
