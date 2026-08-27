package com.qlnv.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

public class AIConfigDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private String provider;

        @JsonProperty("api_key")
        private String apiKey;

        @JsonProperty("chat_model")
        private String chatModel;

        @JsonProperty("embedding_model")
        private String embeddingModel;

        @JsonProperty("top_k")
        private Integer topK;

        @JsonProperty("chunk_size")
        private Integer chunkSize;

        private Integer overlap;
        private Float temperature;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Integer id;
        private String provider;

        @JsonProperty("api_key")
        private String apiKey;

        @JsonProperty("chat_model")
        private String chatModel;

        @JsonProperty("embedding_model")
        private String embeddingModel;

        @JsonProperty("top_k")
        private Integer topK;

        @JsonProperty("chunk_size")
        private Integer chunkSize;

        private Integer overlap;
        private Float temperature;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestKeyRequest {
        @JsonProperty("api_key")
        private String apiKey;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestKeyResponse {
        private Boolean valid;
        private String message;
    }
}
