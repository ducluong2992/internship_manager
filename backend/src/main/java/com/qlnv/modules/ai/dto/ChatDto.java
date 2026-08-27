package com.qlnv.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

public class ChatDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private String message;

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
        private String reply;

        @Builder.Default
        private List<String> sources = new ArrayList<>();
    }
}
