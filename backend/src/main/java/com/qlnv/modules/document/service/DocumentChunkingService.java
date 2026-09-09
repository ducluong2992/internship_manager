package com.qlnv.modules.document.service;

import com.qlnv.modules.document.entity.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Chia văn bản tài liệu thành các chunk có ý nghĩa theo 3 chiến lược ưu tiên:
 * 1. Section-based: detect heading → mỗi section thành 1+ chunk
 * 2. Paragraph-based: split tại dòng trống, merge cho đủ kích thước
 * 3. Sliding window: fallback cuối cùng với overlap
 */
@Service
@Slf4j
public class DocumentChunkingService {

    // Pattern nhận diện tiêu đề section (tiếng Việt văn bản pháp luật/nội quy)
    private static final Pattern SECTION_HEADING = Pattern.compile(
            "(?m)^(" +
            // Điều X., Chương X:, Phần X, Mục X, Khoản X, Phụ lục
            "(?:Điều|Chương|Phần|Mục|Khoản|Phụ\\s+lục)\\s+\\S+[.:].*" +
            // I. Tổng quan / II. Nội dung (Roman numeral headings)
            "|[IVXLCDM]{1,6}[.\\s]+\\p{Lu}.{4,}" +
            // 1. Mục đích / 2) Phạm vi (numbered headings)
            "|\\d{1,2}[.)\\s]+\\p{Lu}.{4,}" +
            // ALL CAPS heading (5+ chars uppercase)
            "|\\p{Lu}{5,}[\\p{Lu}\\s]*" +
            ")"
    );

    /**
     * Chunk tài liệu theo chiến lược tốt nhất có thể.
     *
     * @param text       Toàn bộ text đã extract từ file
     * @param chunkSize  Kích thước tối đa mỗi chunk (ký tự)
     * @param overlap    Số ký tự overlap giữa các chunk liền kề
     * @return Danh sách chunk kèm metadata (sectionTitle, pageNumber ước tính)
     */
    public List<ChunkResult> chunk(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) return List.of();

        // Thử section-based chunking trước
        List<ChunkResult> sectionChunks = chunkBySection(text, chunkSize, overlap);
        if (sectionChunks.size() > 1) {
            log.debug("[Chunking] Section-based: {} chunks", sectionChunks.size());
            return sectionChunks;
        }

        // Fallback: paragraph-based
        List<ChunkResult> paraChunks = chunkByParagraph(text, chunkSize, overlap);
        if (paraChunks.size() > 1) {
            log.debug("[Chunking] Paragraph-based: {} chunks", paraChunks.size());
            return paraChunks;
        }

        // Fallback cuối: sliding window
        log.debug("[Chunking] Sliding window fallback");
        return chunkBySlidingWindow(text, chunkSize, overlap);
    }

    // ─── Section-based ────────────────────────────────────────────────────────

    private List<ChunkResult> chunkBySection(String text, int chunkSize, int overlap) {
        List<ChunkResult> results = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        String currentSection = null;
        StringBuilder currentBuffer = new StringBuilder();
        int estimatedPage = 1;
        int charCount = 0;

        for (String line : lines) {
            // Ước tính số trang (mỗi ~3000 ký tự ~ 1 trang A4)
            charCount += line.length() + 1;
            if (charCount > 3000) {
                estimatedPage++;
                charCount = 0;
            }

            boolean isHeading = SECTION_HEADING.matcher(line.trim()).find() && line.trim().length() < 200;

            if (isHeading && currentBuffer.length() > 50) {
                // Flush buffer trước khi bắt đầu section mới
                flushBuffer(currentBuffer.toString(), currentSection, estimatedPage, chunkSize, overlap, results);
                currentBuffer = new StringBuilder();
                currentSection = line.trim();
                currentBuffer.append(line).append("\n");
            } else {
                if (isHeading) currentSection = line.trim();
                currentBuffer.append(line).append("\n");

                // Chunk lớn quá → flush intermediate
                if (currentBuffer.length() > chunkSize * 2) {
                    flushBuffer(currentBuffer.toString(), currentSection, estimatedPage, chunkSize, overlap, results);
                    // Giữ lại phần overlap
                    String bufStr = currentBuffer.toString();
                    currentBuffer = new StringBuilder(
                            bufStr.substring(Math.max(0, bufStr.length() - overlap))
                    );
                }
            }
        }

        // Flush phần cuối
        if (!currentBuffer.isEmpty()) {
            flushBuffer(currentBuffer.toString(), currentSection, estimatedPage, chunkSize, overlap, results);
        }

        return results;
    }

    private void flushBuffer(String buffer, String sectionTitle, int pageNum, int chunkSize, int overlap,
                             List<ChunkResult> results) {
        String trimmed = buffer.trim();
        if (trimmed.length() < 30) return; // bỏ chunk quá ngắn

        if (trimmed.length() <= chunkSize) {
            results.add(new ChunkResult(trimmed, sectionTitle, pageNum));
        } else {
            // Chia nhỏ hơn với sliding window
            List<ChunkResult> sub = chunkBySlidingWindow(trimmed, chunkSize, overlap);
            for (ChunkResult c : sub) {
                results.add(new ChunkResult(c.content(), sectionTitle, pageNum));
            }
        }
    }

    // ─── Paragraph-based ──────────────────────────────────────────────────────

    private List<ChunkResult> chunkByParagraph(String text, int chunkSize, int overlap) {
        String[] paragraphs = text.split("\\r?\\n\\s*\\r?\\n+");
        List<ChunkResult> results = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        int pageEst = 1;
        int charCount = 0;

        for (String para : paragraphs) {
            String p = para.trim();
            if (p.isEmpty()) continue;

            charCount += p.length();
            if (charCount > 3000) { pageEst++; charCount = 0; }

            if (buffer.length() + p.length() + 2 > chunkSize && !buffer.isEmpty()) {
                results.add(new ChunkResult(buffer.toString().trim(), null, pageEst));
                // overlap: giữ lại phần cuối
                String prev = buffer.toString();
                buffer = new StringBuilder(prev.substring(Math.max(0, prev.length() - overlap)));
            }
            buffer.append(p).append("\n\n");
        }
        if (!buffer.isEmpty()) {
            results.add(new ChunkResult(buffer.toString().trim(), null, pageEst));
        }
        return results;
    }

    // ─── Sliding Window ───────────────────────────────────────────────────────

    private List<ChunkResult> chunkBySlidingWindow(String text, int chunkSize, int overlap) {
        List<ChunkResult> results = new ArrayList<>();
        int start = 0;
        int pageEst = 1;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // Cắt tại ranh giới câu (ưu tiên) hoặc khoảng trắng
            if (end < text.length()) {
                int sentenceBreak = findSentenceBreak(text, start, end);
                if (sentenceBreak > start + overlap) end = sentenceBreak;
            }

            String chunk = text.substring(start, end).trim();
            if (chunk.length() > 20) {
                pageEst = Math.max(1, start / 3000 + 1);
                results.add(new ChunkResult(chunk, null, pageEst));
            }

            start = end - overlap;
            if (start >= text.length() - overlap) break;
        }
        return results;
    }

    private int findSentenceBreak(String text, int from, int to) {
        // Tìm dấu chấm câu gần nhất từ to về from
        for (int i = to; i > from + 100; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                return i + 1;
            }
        }
        // fallback: tìm khoảng trắng
        for (int i = to; i > from + 100; i--) {
            if (text.charAt(i) == ' ') return i;
        }
        return to;
    }

    // ─── Result DTO ───────────────────────────────────────────────────────────

    public record ChunkResult(String content, String sectionTitle, Integer pageNumber) {}
}
