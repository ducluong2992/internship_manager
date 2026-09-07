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

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final AIConfigService aiConfigService;
    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private File findDocumentFile(Document doc) {
        File dir = new File(uploadDir);
        if (!dir.exists()) return null;

        // 1. Check direct filename
        if (doc.getFilename() != null) {
            File f = new File(dir, doc.getFilename());
            if (f.exists()) return f;
        }

        // 2. Check id_filename prefix (legacy Python convention)
        if (doc.getId() != null && doc.getFilename() != null) {
            File f = new File(dir, doc.getId() + "_" + doc.getFilename());
            if (f.exists()) return f;
        }

        // 3. Check title
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
            log.warn("Could not extract text from file: " + file.getAbsolutePath(), e);
        }
        return "";
    }

    public AIConfigDto.TestKeyResponse testApiKey(AIConfigDto.TestKeyRequest req) {
        String apiKey = req != null && req.getApiKey() != null && !req.getApiKey().trim().isEmpty()
                ? req.getApiKey().trim()
                : null;

        if (apiKey == null) {
            AIConfig config = aiConfigService.getOrCreateConfig();
            apiKey = config.getApiKey();
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return AIConfigDto.TestKeyResponse.builder()
                    .valid(false)
                    .success(false)
                    .message("API Key không được để trống")
                    .build();
        }

        String model = req != null && req.getModel() != null && !req.getModel().trim().isEmpty()
                ? req.getModel().trim()
                : "gemini-2.5-flash";

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
                        .valid(true)
                        .success(true)
                        .message("Kết nối Gemini API thành công!")
                        .preview(preview)
                        .build();
            } else {
                return AIConfigDto.TestKeyResponse.builder()
                        .valid(false)
                        .success(false)
                        .message("API Key không hợp lệ hoặc không có quyền truy cập")
                        .build();
            }
        } catch (Exception e) {
            try {
                String fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey.trim();
                ResponseEntity<String> fallbackRes = restTemplate.getForEntity(fallbackUrl, String.class);
                if (fallbackRes.getStatusCode().is2xxSuccessful()) {
                    return AIConfigDto.TestKeyResponse.builder()
                            .valid(true)
                            .success(true)
                            .message("Kết nối Gemini API thành công!")
                            .preview("API Key hợp lệ.")
                            .build();
                }
            } catch (Exception ignored) {}

            return AIConfigDto.TestKeyResponse.builder()
                    .valid(false)
                    .success(false)
                    .message("Không thể kết nối tới Google Gemini: " + e.getMessage())
                    .build();
        }
    }

    public ChatDto.Response chat(ChatDto.Request req) {
        AIConfig config = aiConfigService.getOrCreateConfig();
        String apiKey = config.getApiKey();
        String model = req.getModel() != null && !req.getModel().trim().isEmpty() ? req.getModel().trim() :
                (config.getChatModel() != null && !config.getChatModel().trim().isEmpty() ? config.getChatModel().trim() : "gemini-2.5-flash");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ChatDto.Response.builder()
                    .answer("Hệ thống chưa được cấu hình Google Gemini API Key. Vui lòng vào mục Cấu hình AI để thiết lập API Key.")
                    .reply("Hệ thống chưa được cấu hình Google Gemini API Key. Vui lòng vào mục Cấu hình AI để thiết lập API Key.")
                    .sources(List.of())
                    .build();
        }

        // Load active documents and extract text
        List<Document> activeDocs = documentRepository.findByIsActiveTrue();
        StringBuilder contextBuilder = new StringBuilder();
        List<Map<String, Object>> sources = new ArrayList<>();

        for (Document doc : activeDocs) {
            File docFile = findDocumentFile(doc);
            if (docFile != null && docFile.exists()) {
                String text = extractTextFromFile(docFile);
                if (text != null && !text.trim().isEmpty()) {
                    contextBuilder.append("\n\n--- TÀI LIỆU NỘI BỘ: ").append(doc.getTitle()).append(" ---\n");
                    contextBuilder.append(text.trim());

                    Map<String, Object> src = new HashMap<>();
                    src.put("document_id", doc.getId());
                    src.put("title", doc.getTitle());
                    src.put("filename", doc.getFilename());
                    sources.add(src);
                }
            }
        }

        String fullPrompt;
        if (contextBuilder.length() > 0) {
            fullPrompt = "Bạn là trợ lý AI thông minh, hỗ trợ quản lý nhân sự và thực tập sinh của Trung tâm Viettel.\n" +
                    "Hãy trả lời câu hỏi của người dùng một cách chính xác, thân thiện, lịch sự và dễ hiểu DỰA TRÊN các tài liệu tri thức/nội quy sau đây:\n" +
                    contextBuilder.toString() + "\n\n" +
                    "Nếu thông tin có trong tài liệu, hãy trích dẫn và hướng dẫn rõ ràng.\n" +
                    "Nếu câu hỏi không nằm trong tài liệu, hãy giải đáp phù hợp theo kiến thức chung hoặc hướng dẫn người dùng liên hệ phòng quản trị nhân sự.\n\n" +
                    "Câu hỏi của người dùng: " + req.getMessage();
        } else {
            fullPrompt = "Bạn là trợ lý AI thông minh của Trung tâm Quản lý Thực tập sinh và Nhân sự Viettel.\n" +
                    "Câu hỏi của người dùng: " + req.getMessage();
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();

            Map<String, Object> body = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", List.of(Map.of("text", fullPrompt)));
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
                        .answer(reply)
                        .reply(reply)
                        .modelUsed(model)
                        .sources(sources)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
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
}
