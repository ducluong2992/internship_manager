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

        @Builder.Default
        private List<Map<String, Object>> sources = new ArrayList<>();
    }
}
