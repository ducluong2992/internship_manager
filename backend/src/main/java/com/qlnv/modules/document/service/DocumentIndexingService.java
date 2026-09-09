package com.qlnv.modules.document.service;

import com.qlnv.modules.ai.service.AIConfigService;
import com.qlnv.modules.ai.service.EmbeddingService;
import com.qlnv.modules.document.entity.Document;
import com.qlnv.modules.document.entity.DocumentChunk;
import com.qlnv.modules.document.repository.DocumentChunkRepository;
import com.qlnv.modules.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline bất đồng bộ để chunk và embed tài liệu sau khi upload.
 * Chạy trong thread "ragIndexExecutor" riêng để không block request của user.
 *
 * Luồng:
 * uploadDocument() → triggerIndexing(docId) [async] →
 *   extractText → chunk → embed mỗi chunk → lưu DocumentChunk → cập nhật Document.indexStatus
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexingService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final AIConfigService aiConfigService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Trigger indexing async cho một tài liệu.
     * Gọi sau khi uploadDocument() thành công.
     */
    @Async("ragIndexExecutor")
    @Transactional
    public void indexDocumentAsync(Integer docId) {
        Document doc = documentRepository.findById(docId).orElse(null);
        if (doc == null) {
            log.warn("[Indexing] Không tìm thấy document id={}", docId);
            return;
        }

        log.info("[Indexing] Bắt đầu index document id={} title={}", docId, doc.getTitle());
        doc.setIndexStatus("INDEXING");
        documentRepository.save(doc);

        try {
            // 1. Tìm file vật lý
            File file = findDocumentFile(doc);
            if (file == null || !file.exists()) {
                log.error("[Indexing] Không tìm thấy file cho document id={}", docId);
                doc.setIndexStatus("ERROR");
                documentRepository.save(doc);
                return;
            }

            // 2. Extract text
            String text = extractTextFromFile(file);
            if (text == null || text.isBlank()) {
                log.warn("[Indexing] Extract text rỗng cho document id={}", docId);
                doc.setIndexStatus("ERROR");
                documentRepository.save(doc);
                return;
            }

            // 3. Lấy config chunking
            var config = aiConfigService.getOrCreateConfig();
            int chunkSize = config.getChunkSize() != null ? config.getChunkSize() : 1000;
            int overlap   = config.getOverlap()    != null ? config.getOverlap()    : 150;
            String embeddingModel = config.getEmbeddingModel() != null
                    ? config.getEmbeddingModel() : "text-embedding-004";

            // 4. Xóa chunks cũ (nếu re-index)
            chunkRepository.deleteByDocumentId(docId);

            // 5. Chunk text
            List<DocumentChunkingService.ChunkResult> chunkResults =
                    chunkingService.chunk(text, chunkSize, overlap);
            log.info("[Indexing] document id={} → {} chunks", docId, chunkResults.size());

            // 6. Embed từng chunk và lưu
            List<DocumentChunk> chunks = new ArrayList<>();
            for (int i = 0; i < chunkResults.size(); i++) {
                var cr = chunkResults.get(i);

                float[] vector = embeddingService.embed(cr.content());
                String embeddingJson = vector != null ? embeddingService.vectorToJson(vector) : null;

                DocumentChunk chunk = DocumentChunk.builder()
                        .documentId(docId)
                        .chunkIndex(i)
                        .sectionTitle(cr.sectionTitle())
                        .pageNumber(cr.pageNumber())
                        .content(cr.content())
                        .contentLength(cr.content().length())
                        .embedding(embeddingJson)
                        .embeddingModel(embeddingModel)
                        .build();

                chunks.add(chunk);

                // Lưu batch mỗi 10 chunk để tránh OOM
                if (chunks.size() >= 10) {
                    chunkRepository.saveAll(chunks);
                    chunks.clear();
                }

                // Delay nhỏ để tránh rate limit Gemini Embedding API (~1500 RPM free tier)
                if (i > 0 && i % 30 == 0) {
                    Thread.sleep(1500);
                }
            }
            if (!chunks.isEmpty()) {
                chunkRepository.saveAll(chunks);
            }

            // 7. Cập nhật trạng thái Document
            doc.setIndexStatus("INDEXED");
            doc.setChunkCount(chunkResults.size());
            doc.setIndexedAt(LocalDateTime.now());
            documentRepository.save(doc);

            log.info("[Indexing] Hoàn thành document id={}, {} chunks embedded", docId, chunkResults.size());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Indexing] Bị interrupt, document id={}", docId);
            doc.setIndexStatus("PENDING");
            documentRepository.save(doc);
        } catch (Exception e) {
            log.error("[Indexing] Lỗi khi index document id={}: {}", docId, e.getMessage(), e);
            doc.setIndexStatus("ERROR");
            documentRepository.save(doc);
        }
    }

    // ─── File Helpers (giống GeminiService) ──────────────────────────────────

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
        }
        return null;
    }

    private String extractTextFromFile(File file) {
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
            log.warn("[Indexing] Không extract được text từ {}: {}", file.getName(), e.getMessage());
        }
        return "";
    }
}
