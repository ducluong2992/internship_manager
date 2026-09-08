package com.qlnv.modules.auth.service;

import com.qlnv.common.exception.ApiException;
import com.qlnv.common.security.JwtTokenProvider;
import com.qlnv.modules.auth.dto.ChangePasswordRequest;
import com.qlnv.modules.auth.dto.LoginRequest;
import com.qlnv.modules.auth.dto.TokenResponse;
import com.qlnv.modules.auth.entity.Account;
import com.qlnv.modules.auth.repository.AccountRepository;
import com.qlnv.modules.user.entity.User;
import com.qlnv.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public TokenResponse login(LoginRequest req) {
        Account account = accountRepository.findByUsername(req.getUsername().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(req.getPassword(), account.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không chính xác");
        }

        User user = userRepository.findById(account.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin người dùng"));

        if ("intern".equalsIgnoreCase(user.getUserType()) || "tts".equalsIgnoreCase(user.getUserType())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Thực tập sinh không có tài khoản truy cập hệ thống");
        }

        if (user.getAccountStatus() != null && user.getAccountStatus() == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa. Vui lòng liên hệ Admin.");
        }

        String token = tokenProvider.generateToken(user.getId(), account.getUsername(), user.getRole());

        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("bearer")
                .role(user.getRole())
                .userId(user.getId())
                .fullName(user.getFullName())
                .userType(user.getUserType())
                .build();
    }

    @Transactional
    public Map<String, String> changePassword(Integer userId, ChangePasswordRequest req) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        if (!passwordEncoder.matches(req.getOldPassword(), account.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không chính xác");
        }

        account.setPassword(passwordEncoder.encode(req.getNewPassword()));
        accountRepository.save(account);

        return Map.of("message", "Đổi mật khẩu thành công");
    }
}
