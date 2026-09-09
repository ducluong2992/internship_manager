package com.qlnv.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlnv.modules.ai.entity.AIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Gọi Google Gemini Embedding API (text-embedding-004) để chuyển văn bản thành vector float[768].
 * Endpoint: POST /v1beta/models/{model}:embedContent
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final AIConfigService aiConfigService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_EMBED_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent?key=%s";

    /**
     * Tạo embedding vector cho một đoạn text.
     *
     * @param text  Văn bản cần embed
     * @return float[768] hoặc null nếu lỗi
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) return null;

        AIConfig config = aiConfigService.getOrCreateConfig();
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[EmbeddingService] API Key chưa được cấu hình, bỏ qua embedding.");
            return null;
        }

        String model = config.getEmbeddingModel() != null && !config.getEmbeddingModel().isBlank()
                ? config.getEmbeddingModel() : "text-embedding-004";

        String url = String.format(GEMINI_EMBED_URL, model, apiKey.trim());

        // Cắt bớt nếu text quá dài (API limit ~2048 tokens ~ 8000 chars)
        String trimmedText = text.length() > 8000 ? text.substring(0, 8000) : text;

        try {
            Map<String, Object> body = Map.of(
                    "model", "models/" + model,
                    "content", Map.of("parts", List.of(Map.of("text", trimmedText)))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[EmbeddingService] HTTP {} từ Gemini Embedding API", response.getStatusCode());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode values = root.path("embedding").path("values");
            if (values.isMissingNode() || !values.isArray()) {
                log.warn("[EmbeddingService] Không tìm thấy embedding.values trong response");
                return null;
            }

            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = (float) values.get(i).asDouble();
            }
            return vector;

        } catch (Exception e) {
            log.error("[EmbeddingService] Lỗi gọi Embedding API: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Chuyển float[] thành JSON string "[0.1, -0.2, ...]" để lưu vào SQLite.
     */
    public String vectorToJson(float[] vector) {
        if (vector == null) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Parse JSON string thành float[].
     */
    public float[] jsonToVector(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) return null;
            float[] vector = new float[node.size()];
            for (int i = 0; i < node.size(); i++) {
                vector[i] = (float) node.get(i).asDouble();
            }
            return vector;
        } catch (Exception e) {
            log.warn("[EmbeddingService] Không parse được embedding JSON: {}", e.getMessage());
            return null;
        }
    }
}
