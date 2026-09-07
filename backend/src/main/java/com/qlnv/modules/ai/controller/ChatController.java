package com.qlnv.modules.ai.controller;

import com.qlnv.modules.ai.dto.AIConfigDto;
import com.qlnv.modules.ai.dto.ChatDto;
import com.qlnv.modules.ai.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/chat", "/api/chat"})
@RequiredArgsConstructor
public class ChatController {

    private final GeminiService geminiService;

    @PostMapping({"", "/"})
    public ResponseEntity<ChatDto.Response> chat(@RequestBody ChatDto.Request req) {
        return ResponseEntity.ok(geminiService.chat(req));
    }

    @PostMapping("/test-key")
    public ResponseEntity<AIConfigDto.TestKeyResponse> testKey(@RequestBody(required = false) AIConfigDto.TestKeyRequest req) {
        return ResponseEntity.ok(geminiService.testApiKey(req));
    }
}
