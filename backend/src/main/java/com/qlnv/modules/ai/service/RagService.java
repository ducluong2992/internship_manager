package com.qlnv.modules.ai.service;

import com.qlnv.modules.document.entity.DocumentChunk;
import com.qlnv.modules.document.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) engine.
 * Tìm các chunk tài liệu liên quan nhất với câu hỏi bằng cosine similarity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;

    /**
     * Tìm top-K chunks liên quan nhất với câu hỏi.
     *
     * @param question    Câu hỏi của người dùng
     * @param activeDocIds Danh sách ID tài liệu đang active (is_active = true)
     * @param topK        Số chunk cần lấy
     * @return Danh sách ScoredChunk, sorted giảm dần theo score
     */
    public List<ScoredChunk> semanticSearch(String question, List<Integer> activeDocIds, int topK) {
        if (question == null || question.isBlank() || activeDocIds == null || activeDocIds.isEmpty()) {
            return List.of();
        }

        // 1. Embed câu hỏi
        float[] queryVec = embeddingService.embed(question);
        if (queryVec == null) {
            log.warn("[RAG] Không embed được câu hỏi, fallback về empty result");
            return List.of();
        }

        // 2. Load chunks đã có embedding của các tài liệu active
        List<DocumentChunk> candidates = chunkRepository.findEmbeddedByDocumentIdIn(activeDocIds);
        if (candidates.isEmpty()) {
            log.info("[RAG] Không có chunk nào đã được embed (có thể tài liệu chưa index xong)");
            return List.of();
        }

        log.debug("[RAG] Tính cosine similarity với {} chunk candidates, topK={}", candidates.size(), topK);

        // 3. Tính cosine similarity với mỗi chunk
        List<ScoredChunk> scored = new ArrayList<>();
        for (DocumentChunk chunk : candidates) {
            float[] chunkVec = embeddingService.jsonToVector(chunk.getEmbedding());
            if (chunkVec == null) continue;

            double score = cosineSimilarity(queryVec, chunkVec);
            scored.add(new ScoredChunk(chunk, score));
        }

        // 4. Sort DESC → lấy top-K
        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        List<ScoredChunk> topResults = scored.stream().limit(topK).collect(Collectors.toList());

        log.debug("[RAG] Top-{} chunks: scores = {}",
                topResults.size(),
                topResults.stream().map(s -> String.format("%.3f", s.score())).collect(Collectors.joining(", ")));

        return topResults;
    }

    /**
     * Cosine similarity giữa 2 vector.
     * Trả về [-1, 1], càng gần 1 càng tương đồng.
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ─── Result DTO ───────────────────────────────────────────────────────────

    public record ScoredChunk(DocumentChunk chunk, double score) {
        /** Trích xuất excerpt ngắn (tối đa 200 ký tự) để hiển thị trong UI. */
        public String excerpt() {
            String content = chunk.getContent();
            if (content == null) return "";
            String trimmed = content.trim().replaceAll("\\s+", " ");
            return trimmed.length() > 200 ? trimmed.substring(0, 200) + "…" : trimmed;
        }
    }
}
