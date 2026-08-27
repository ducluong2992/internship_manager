package com.qlnv.modules.user.controller;

import com.qlnv.common.security.UserPrincipal;
import com.qlnv.modules.user.dto.UserRequestDto;
import com.qlnv.modules.user.dto.UserResponse;
import com.qlnv.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getMe(principal.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserRequestDto req) {
        return ResponseEntity.ok(userService.updateUser(principal.getId(), req));
    }
}
