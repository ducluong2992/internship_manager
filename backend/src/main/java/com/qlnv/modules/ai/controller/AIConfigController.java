package com.qlnv.modules.ai.controller;

import com.qlnv.modules.ai.dto.AIConfigDto;
import com.qlnv.modules.ai.service.AIConfigService;
import com.qlnv.modules.ai.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-config")
@RequiredArgsConstructor
public class AIConfigController {

    private final AIConfigService aiConfigService;
    private final GeminiService geminiService;

    @GetMapping
    public ResponseEntity<AIConfigDto.Response> getConfig() {
        return ResponseEntity.ok(aiConfigService.getConfigResponse());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AIConfigDto.Response> saveConfig(@RequestBody AIConfigDto.Request req) {
        return ResponseEntity.ok(aiConfigService.saveConfig(req));
    }

    @PostMapping("/test-key")
    public ResponseEntity<AIConfigDto.TestKeyResponse> testKey(@RequestBody AIConfigDto.TestKeyRequest req) {
        return ResponseEntity.ok(geminiService.testApiKey(req.getApiKey()));
    }
}
