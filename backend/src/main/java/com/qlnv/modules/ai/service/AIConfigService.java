package com.qlnv.modules.ai.service;

import com.qlnv.modules.ai.dto.AIConfigDto;
import com.qlnv.modules.ai.entity.AIConfig;
import com.qlnv.modules.ai.repository.AIConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AIConfigService {

    private final AIConfigRepository aiConfigRepository;

    public AIConfig getOrCreateConfig() {
        return aiConfigRepository.findFirstByOrderByIdAsc().orElseGet(() -> {
            AIConfig config = AIConfig.builder()
                    .provider("Google Gemini")
                    .chatModel("gemini-2.5-flash")
                    .embeddingModel("text-embedding-004")
                    .topK(5)
                    .chunkSize(1000)
                    .overlap(150)
                    .temperature(0.2f)
                    .build();
            return aiConfigRepository.save(config);
        });
    }

    public AIConfigDto.Response getConfigResponse() {
        AIConfig c = getOrCreateConfig();
        return AIConfigDto.Response.builder()
                .id(c.getId())
                .provider(c.getProvider())
                .apiKey(c.getApiKey())
                .chatModel(c.getChatModel())
                .embeddingModel(c.getEmbeddingModel())
                .topK(c.getTopK())
                .chunkSize(c.getChunkSize())
                .overlap(c.getOverlap())
                .temperature(c.getTemperature())
                .build();
    }

    @Transactional
    public AIConfigDto.Response saveConfig(AIConfigDto.Request req) {
        AIConfig c = getOrCreateConfig();
        if (req.getProvider() != null) c.setProvider(req.getProvider());
        if (req.getApiKey() != null) c.setApiKey(req.getApiKey());
        if (req.getChatModel() != null) c.setChatModel(req.getChatModel());
        if (req.getEmbeddingModel() != null) c.setEmbeddingModel(req.getEmbeddingModel());
        if (req.getTopK() != null) c.setTopK(req.getTopK());
        if (req.getChunkSize() != null) c.setChunkSize(req.getChunkSize());
        if (req.getOverlap() != null) c.setOverlap(req.getOverlap());
        if (req.getTemperature() != null) c.setTemperature(req.getTemperature());

        c = aiConfigRepository.save(c);

        return AIConfigDto.Response.builder()
                .id(c.getId())
                .provider(c.getProvider())
                .apiKey(c.getApiKey())
                .chatModel(c.getChatModel())
                .embeddingModel(c.getEmbeddingModel())
                .topK(c.getTopK())
                .chunkSize(c.getChunkSize())
                .overlap(c.getOverlap())
                .temperature(c.getTemperature())
                .build();
    }
}
