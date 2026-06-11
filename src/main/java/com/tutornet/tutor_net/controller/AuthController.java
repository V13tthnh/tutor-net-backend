package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.AuthRequest.*;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.AuthResponse;
import com.tutornet.tutor_net.dto.response.TokenResponse;
import com.tutornet.tutor_net.dto.response.UserResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // Cấu hình CORS linh hoạt cho các port frontend khác nhau
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerClient(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmailToken(token);
        return ResponseEntity.ok("Xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginClient(@RequestBody LoginRequest request) {
        AuthResponse response = authService.loginClient(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> loginAdmin(@RequestBody LoginRequest request) {
        AuthResponse response = authService.loginAdmin(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        // Lấy email từ principal, không dùng user object từ cache
        String email = userDetails.getUsername();
        return ResponseEntity.ok(authService.getCurrentUser(email));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(
                authService.refreshToken(request)
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        // Luôn trả 200 dù email có tồn tại hay không (tránh user enumeration)
        return ResponseEntity.ok(ApiResponse.ok(
                "Chúng tôi đã gửi link để đặt lại mật khẩu, hãy kiểm tra email của bạn"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Đặt lại mật khẩu thành công"));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authHeader)
    {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body("Token không hợp lệ");
        }
        String token = authHeader.substring(7);
    
        authService.logout(token);
        return ResponseEntity.ok("Đăng xuất thành công");
    }
}