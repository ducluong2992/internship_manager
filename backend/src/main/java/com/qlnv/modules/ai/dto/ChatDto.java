package com.qlnv.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private String message;
        private String model;

        @JsonProperty("session_id")
        private String sessionId;

        @Builder.Default
        private List<Object> history = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String answer;
        private String reply;

        @JsonProperty("model_used")
        private String modelUsed;

        /** Danh sách nguồn trích dẫn chi tiết từ RAG */
        @Builder.Default
        private List<ChunkSource> sources = new ArrayList<>();

        /**
         * true  = RAG đã hoạt động và tìm được chunks liên quan
         * false = fallback về mode thông thường (chưa index hoặc không có tài liệu active)
         */
        @JsonProperty("rag_used")
        @Builder.Default
        private boolean ragUsed = false;
    }

    /**
     * Nguồn trích dẫn từ một chunk tài liệu cụ thể.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChunkSource {
        @JsonProperty("document_id")
        private Integer documentId;

        private String title;        // Tên tài liệu gốc

        @JsonProperty("section_title")
        private String sectionTitle; // Tiêu đề section chứa chunk

        @JsonProperty("page_number")
        private Integer pageNumber;  // Số trang ước tính

        private String excerpt;      // Đoạn văn gốc (tối đa 200 ký tự)

        private Double score;        // Cosine similarity score [0, 1]
    }
}
