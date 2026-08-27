package com.qlnv.modules.auth.controller;

import com.qlnv.common.security.UserPrincipal;
import com.qlnv.modules.auth.dto.ChangePasswordRequest;
import com.qlnv.modules.auth.dto.LoginRequest;
import com.qlnv.modules.auth.dto.TokenResponse;
import com.qlnv.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest req) {
        return ResponseEntity.ok(authService.changePassword(principal.getId(), req));
    }
}
