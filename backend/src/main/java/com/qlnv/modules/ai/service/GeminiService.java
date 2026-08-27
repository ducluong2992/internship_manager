package com.qlnv.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlnv.modules.ai.dto.AIConfigDto;
import com.qlnv.modules.ai.dto.ChatDto;
import com.qlnv.modules.ai.entity.AIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final AIConfigService aiConfigService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIConfigDto.TestKeyResponse testApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return AIConfigDto.TestKeyResponse.builder()
                    .valid(false)
                    .message("API Key không được để trống")
                    .build();
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey.trim();
            ResponseEntity<String> res = restTemplate.getForEntity(url, String.class);
            if (res.getStatusCode().is2xxSuccessful()) {
                return AIConfigDto.TestKeyResponse.builder()
                        .valid(true)
                        .message("Kết nối Gemini API thành công!")
                        .build();
            } else {
                return AIConfigDto.TestKeyResponse.builder()
                        .valid(false)
                        .message("API Key không hợp lệ hoặc không có quyền truy cập")
                        .build();
            }
        } catch (Exception e) {
            return AIConfigDto.TestKeyResponse.builder()
                    .valid(false)
                    .message("Không thể kết nối tới Google Gemini: " + e.getMessage())
                    .build();
        }
    }

    public ChatDto.Response chat(ChatDto.Request req) {
        AIConfig config = aiConfigService.getOrCreateConfig();
        String apiKey = config.getApiKey();
        String model = config.getChatModel() != null ? config.getChatModel() : "gemini-2.5-flash";

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ChatDto.Response.builder()
                    .reply("Hệ thống chưa được cấu hình Google Gemini API Key. Vui lòng vào mục Cấu hình AI để thiết lập API Key.")
                    .sources(List.of())
                    .build();
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();

            Map<String, Object> body = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", List.of(Map.of("text", req.getMessage())));
            contents.add(userContent);
            body.put("contents", contents);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                String reply = textNode.isMissingNode() ? "Không nhận được phản hồi từ AI." : textNode.asText();

                return ChatDto.Response.builder()
                        .reply(reply)
                        .sources(List.of())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            return ChatDto.Response.builder()
                    .reply("Lỗi khi kết nối tới Trợ lý AI Gemini: " + e.getMessage())
                    .sources(List.of())
                    .build();
        }

        return ChatDto.Response.builder()
                .reply("Không thể xử lý yêu cầu lúc này.")
                .sources(List.of())
                .build();
    }
}
