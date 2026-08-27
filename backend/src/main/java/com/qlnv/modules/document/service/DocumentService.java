package com.qlnv.modules.document.service;

import com.qlnv.common.exception.ApiException;
import com.qlnv.modules.document.dto.DocumentResponse;
import com.qlnv.modules.document.entity.Document;
import com.qlnv.modules.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String uploadedBy) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File không được để trống");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String ext = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx >= 0) {
            ext = originalFilename.substring(dotIdx);
        }

        String storedFilename = UUID.randomUUID() + ext;

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File targetFile = new File(dir, storedFilename);
        try (InputStream is = file.getInputStream(); FileOutputStream fos = new FileOutputStream(targetFile)) {
            is.transferTo(fos);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu file: " + e.getMessage());
        }

        Document doc = Document.builder()
                .title(originalFilename)
                .filename(storedFilename)
                .status("READY")
                .isActive(true)
                .uploadedBy(uploadedBy)
                .createdAt(LocalDateTime.now())
                .build();

        doc = documentRepository.save(doc);
        return toResponse(doc);
    }

    @Transactional
    public DocumentResponse toggleActive(Integer docId) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu"));

        doc.setIsActive(!Boolean.TRUE.equals(doc.getIsActive()));
        return toResponse(documentRepository.save(doc));
    }

    @Transactional
    public void deleteDocument(Integer docId) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu"));

        try {
            File f = new File(uploadDir, doc.getFilename());
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception e) {
            log.warn("Could not delete file from disk: " + doc.getFilename(), e);
        }

        documentRepository.delete(doc);
    }

    public DocumentResponse toResponse(Document doc) {
        if (doc == null) return null;
        return DocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .filename(doc.getFilename())
                .status(doc.getStatus())
                .isActive(doc.getIsActive())
                .uploadedBy(doc.getUploadedBy())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
