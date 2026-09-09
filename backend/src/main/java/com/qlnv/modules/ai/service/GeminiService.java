package com.qlnv.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlnv.modules.ai.dto.AIConfigDto;
import com.qlnv.modules.ai.dto.ChatDto;
import com.qlnv.modules.ai.entity.AIConfig;
import com.qlnv.modules.document.entity.Document;
import com.qlnv.modules.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final AIConfigService aiConfigService;
    private final DocumentRepository documentRepository;
    private final RagService ragService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // ─── Test API Key ──────────────────────────────────────────────────────────

    public AIConfigDto.TestKeyResponse testApiKey(AIConfigDto.TestKeyRequest req) {
        String apiKey = req != null && req.getApiKey() != null && !req.getApiKey().trim().isEmpty()
                ? req.getApiKey().trim()
                : null;

        AIConfig config = aiConfigService.getOrCreateConfig();
        if (apiKey == null) {
            apiKey = config.getApiKey();
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return AIConfigDto.TestKeyResponse.builder()
                    .valid(false).success(false)
                    .message("API Key không được để trống")
                    .build();
        }

        String model = req != null && req.getModel() != null && !req.getModel().trim().isEmpty()
                ? req.getModel().trim()
                : (config.getChatModel() != null ? config.getChatModel() : "gemini-2.5-flash");

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", "Xin chào"))))
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> res = restTemplate.postForEntity(url, entity, String.class);
            if (res.getStatusCode().is2xxSuccessful()) {
                String preview = "Đã kết nối tốt với " + model;
                try {
                    JsonNode root = objectMapper.readTree(res.getBody());
                    JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                    if (!textNode.isMissingNode()) {
                        preview = textNode.asText().trim();
                        if (preview.length() > 60) preview = preview.substring(0, 60) + "...";
                    }
                } catch (Exception ignored) {}
                return AIConfigDto.TestKeyResponse.builder()
                        .valid(true).success(true)
                        .message("Kết nối Gemini API thành công!")
                        .preview(preview).build();
            } else {
                return AIConfigDto.TestKeyResponse.builder()
                        .valid(false).success(false)
                        .message("API Key không hợp lệ hoặc không có quyền truy cập")
                        .build();
            }
        } catch (Exception e) {
            try {
                String fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey.trim();
                ResponseEntity<String> fallbackRes = restTemplate.getForEntity(fallbackUrl, String.class);
                if (fallbackRes.getStatusCode().is2xxSuccessful()) {
                    return AIConfigDto.TestKeyResponse.builder()
                            .valid(true).success(true)
                            .message("Kết nối Gemini API thành công!")
                            .preview("API Key hợp lệ.").build();
                }
            } catch (Exception ignored) {}
            return AIConfigDto.TestKeyResponse.builder()
                    .valid(false).success(false)
                    .message("Không thể kết nối tới Google Gemini: " + e.getMessage())
                    .build();
        }
    }

    // ─── Chat với RAG ─────────────────────────────────────────────────────────

    public ChatDto.Response chat(ChatDto.Request req) {
        AIConfig config = aiConfigService.getOrCreateConfig();
        String apiKey = config.getApiKey();
        String model = req.getModel() != null && !req.getModel().trim().isEmpty()
                ? req.getModel().trim()
                : (config.getChatModel() != null ? config.getChatModel() : "gemini-2.5-flash");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ChatDto.Response.builder()
                    .answer("Hệ thống chưa được cấu hình Google Gemini API Key. Vui lòng vào mục Cấu hình AI để thiết lập API Key.")
                    .reply("Hệ thống chưa được cấu hình Google Gemini API Key.")
                    .sources(List.of())
                    .build();
        }

        // ── Lấy danh sách tài liệu active ──
        List<Document> activeDocs = documentRepository.findByIsActiveTrue();
        List<Integer> activeDocIds = activeDocs.stream().map(Document::getId).collect(Collectors.toList());

        // ── Xác định có tài liệu INDEXED không ──
        boolean hasIndexedDocs = activeDocs.stream()
                .anyMatch(d -> "INDEXED".equals(d.getIndexStatus()));

        String fullPrompt;
        List<ChatDto.ChunkSource> sources = new ArrayList<>();
        boolean ragUsed = false;

        if (hasIndexedDocs) {
            // ═══ RAG PATH ═══
            int topK = config.getTopK() != null ? config.getTopK() : 5;
            List<RagService.ScoredChunk> topChunks = ragService.semanticSearch(req.getMessage(), activeDocIds, topK);

            if (!topChunks.isEmpty()) {
                ragUsed = true;

                // Build context từ top-K chunks
                StringBuilder contextBuilder = new StringBuilder();
                Map<Integer, String> docTitleMap = activeDocs.stream()
                        .collect(Collectors.toMap(Document::getId, Document::getTitle));

                for (int i = 0; i < topChunks.size(); i++) {
                    RagService.ScoredChunk sc = topChunks.get(i);
                    String docTitle = docTitleMap.getOrDefault(sc.chunk().getDocumentId(), "Tài liệu");
                    Integer page = sc.chunk().getPageNumber();

                    contextBuilder.append("\n---\n").append(sc.chunk().getContent()).append("\n");

                    // Build source response
                    sources.add(ChatDto.ChunkSource.builder()
                            .documentId(sc.chunk().getDocumentId())
                            .title(docTitle)
                            .sectionTitle(sc.chunk().getSectionTitle())
                            .pageNumber(page)
                            .excerpt(sc.excerpt())
                            .score(Math.round(sc.score() * 1000.0) / 1000.0)
                            .build());
                }

                fullPrompt = "Bạn là trợ lý AI thông minh của Viettel, hỗ trợ nhân sự và thực tập sinh.\n" +
                        "Hãy trả lời câu hỏi của người dùng một cách tự nhiên, ngắn gọn, trực tiếp và chính xác dựa trên các thông tin dưới đây.\n" +
                        "Quy tắc bắt buộc khi trả lời:\n" +
                        "1. Trả lời thẳng vào nội dung câu hỏi, lịch sự, rõ ràng.\n" +
                        "2. TUYỆT ĐỐI KHÔNG ghi nguồn, không ghi chú thích xuất xứ, không ghi '(Nguồn: ...)' hay tên tài liệu.\n" +
                        "3. Định dạng Markdown: in đậm (**từ khóa**) và danh sách gạch đầu dòng rõ ràng.\n" +
                        "4. Nếu thông tin không có trong tài liệu, hãy trả lời: \"Tôi không tìm thấy thông tin này trong tài liệu nội bộ. Bạn vui lòng liên hệ phòng HCNS để được giải đáp.\"\n\n" +
                        "=== THÔNG TIN THAM KHẢO ===\n" +
                        contextBuilder.toString() +
                        "\n=== CÂU HỎI CỦA NGƯỜI DÙNG ===\n" +
                        req.getMessage();

                log.info("[GeminiService] RAG mode: {} chunks, topScore={:.3f}",
                        topChunks.size(), topChunks.get(0).score());

            } else {
                // Có indexed docs nhưng semantic search không tìm được chunk nào (edge case)
                fullPrompt = buildFallbackPrompt(req.getMessage());
                log.info("[GeminiService] RAG search returned 0 chunks, falling back to general prompt");
            }

        } else if (!activeDocs.isEmpty()) {
            // ═══ FALLBACK: có tài liệu nhưng chưa index xong ═══
            // Dùng brute-force text stuffing (cũ) với warning
            StringBuilder contextBuilder = new StringBuilder();
            for (Document doc : activeDocs) {
                File docFile = findDocumentFile(doc);
                if (docFile != null && docFile.exists()) {
                    String text = extractTextFromFile(docFile);
                    if (text != null && !text.isBlank()) {
                        // Giới hạn 3000 chars mỗi doc để tránh prompt quá dài
                        String trimmedText = text.length() > 3000 ? text.substring(0, 3000) + "..." : text;
                        contextBuilder.append("\n---\n").append(trimmedText).append("\n");
                        sources.add(ChatDto.ChunkSource.builder()
                                .documentId(doc.getId())
                                .title(doc.getTitle())
                                .excerpt("(Tài liệu chưa được index đầy đủ)")
                                .score(null)
                                .build());
                    }
                }
            }

            fullPrompt = contextBuilder.length() > 0
                    ? "Bạn là trợ lý AI Viettel. Hãy trả lời câu hỏi trực tiếp dựa trên tài liệu sau (không ghi nguồn):\n" +
                    contextBuilder + "\nCâu hỏi: " + req.getMessage()
                    : buildFallbackPrompt(req.getMessage());

        } else {
            // ═══ NO DOCS: không có tài liệu nào active ═══
            fullPrompt = buildFallbackPrompt(req.getMessage());
        }

        // ── Gọi Gemini Chat API ──
        return callGeminiChat(fullPrompt, model, apiKey, sources, ragUsed);
    }

    private String buildFallbackPrompt(String message) {
        return "Bạn là trợ lý AI thông minh của Trung tâm Quản lý Thực tập sinh và Nhân sự Viettel.\n" +
                "Hãy trả lời câu hỏi trực tiếp, lịch sự, định dạng Markdown đẹp mắt (in đậm ý chính, gạch đầu dòng rõ ràng, không ghi nguồn).\n" +
                "Câu hỏi của người dùng: " + message;
    }

    private String cleanAnswerText(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)\\(\\s*Nguồn:[^)]*\\)", "")
                   .replaceAll("(?i)\\(\\s*Đoạn\\s*\\d+[^)]*\\)", "")
                   .replaceAll("(?i)(?:^|\\n)\\s*Nguồn:.*?(?=\\n|$)", "")
                   .replaceAll("(?i)\\[ĐOẠN\\s*\\d+\\]", "")
                   .trim();
    }

    private ChatDto.Response callGeminiChat(String prompt, String model, String apiKey,
                                             List<ChatDto.ChunkSource> sources, boolean ragUsed) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();

            Map<String, Object> body = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", List.of(Map.of("text", prompt)));
            contents.add(userContent);
            body.put("contents", contents);

            // Generation config
            Map<String, Object> genConfig = new HashMap<>();
            AIConfig config = aiConfigService.getOrCreateConfig();
            if (config.getTemperature() != null) {
                genConfig.put("temperature", config.getTemperature());
            }
            genConfig.put("maxOutputTokens", 2048);
            body.put("generationConfig", genConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                String rawReply = textNode.isMissingNode() ? "Không nhận được phản hồi từ AI." : textNode.asText();
                String reply = cleanAnswerText(rawReply);

                return ChatDto.Response.builder()
                        .answer(reply)
                        .reply(reply)
                        .modelUsed(model)
                        .sources(sources)
                        .ragUsed(ragUsed)
                        .build();
            }

        } catch (Exception e) {
            log.error("[GeminiService] Lỗi gọi Gemini Chat API: {}", e.getMessage(), e);
            return ChatDto.Response.builder()
                    .answer("Lỗi khi kết nối tới Trợ lý AI Gemini: " + e.getMessage())
                    .reply("Lỗi khi kết nối tới Trợ lý AI Gemini: " + e.getMessage())
                    .sources(List.of())
                    .build();
        }

        return ChatDto.Response.builder()
                .answer("Không thể xử lý yêu cầu lúc này.")
                .reply("Không thể xử lý yêu cầu lúc này.")
                .sources(List.of())
                .build();
    }

    // ─── Legacy file helpers (dùng cho fallback khi chưa index) ───────────────

    private File findDocumentFile(Document doc) {
        File dir = new File(uploadDir);
        if (!dir.exists()) return null;
        if (doc.getFilename() != null) {
            File f = new File(dir, doc.getFilename());
            if (f.exists()) return f;
        }
        if (doc.getId() != null && doc.getFilename() != null) {
            File f = new File(dir, doc.getId() + "_" + doc.getFilename());
            if (f.exists()) return f;
        }
        if (doc.getTitle() != null) {
            File f = new File(dir, doc.getTitle());
            if (f.exists()) return f;
            if (doc.getId() != null) {
                File f2 = new File(dir, doc.getId() + "_" + doc.getTitle());
                if (f2.exists()) return f2;
            }
        }
        return null;
    }

    private String extractTextFromFile(File file) {
        if (file == null || !file.exists()) return "";
        String name = file.getName().toLowerCase();
        try {
            if (name.endsWith(".docx")) {
                try (FileInputStream fis = new FileInputStream(file);
                     XWPFDocument document = new XWPFDocument(fis);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    return extractor.getText();
                }
            } else if (name.endsWith(".pdf")) {
                try (PDDocument pdDoc = Loader.loadPDF(file)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(pdDoc);
                }
            } else if (name.endsWith(".txt")) {
                return Files.readString(file.toPath());
            }
        } catch (Exception e) {
            log.warn("Could not extract text from file: {}", file.getAbsolutePath(), e);
        }
        return "";
    }
}
