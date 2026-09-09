package com.qlnv.modules.document.controller;

import com.qlnv.common.security.UserPrincipal;
import com.qlnv.modules.document.dto.DocumentResponse;
import com.qlnv.modules.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/documents", "/api/documents"})
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping({"", "/"})
    public ResponseEntity<List<DocumentResponse>> getDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        String uploadedBy = principal != null ? principal.getEmployeeCode() : "admin";
        return ResponseEntity.ok(documentService.uploadDocument(file, uploadedBy));
    }

    @PutMapping("/{docId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> toggleDocument(@PathVariable("docId") Integer docId) {
        return ResponseEntity.ok(documentService.toggleActive(docId));
    }

    @DeleteMapping("/{docId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable("docId") Integer docId) {
        documentService.deleteDocument(docId);
        return ResponseEntity.ok(Map.of("message", "Xóa tài liệu thành công"));
    }
}
