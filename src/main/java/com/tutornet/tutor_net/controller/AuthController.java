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
    private final com.tutornet.tutor_net.service.RateLimiterService rateLimiterService;

    public AuthController(AuthService authService, com.tutornet.tutor_net.service.RateLimiterService rateLimiterService) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @RequestBody RegisterRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest
    ) {
        String ip = servletRequest.getRemoteAddr();
        String limitKey = "register:ip:" + ip;
        if (rateLimiterService.isBlocked(limitKey)) {
            throw new com.tutornet.tutor_net.exception.BusinessException(
                    "Bạn đã đăng ký quá nhiều lần từ IP này. Vui lòng thử lại sau 15 phút.");
        }
        rateLimiterService.recordFailedAttempt(limitKey, 5, 15);

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
            @Valid @RequestBody ForgotPasswordRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest
    ) {
        String ip = servletRequest.getRemoteAddr();
        String limitKey = "forgot-password:ip:" + ip;
        if (rateLimiterService.isBlocked(limitKey)) {
            throw new com.tutornet.tutor_net.exception.BusinessException(
                    "Bạn đã yêu cầu đặt lại mật khẩu quá nhiều lần. Vui lòng thử lại sau 15 phút.");
        }
        rateLimiterService.recordFailedAttempt(limitKey, 5, 15);

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
    public ResponseEntity<String> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok("Đăng xuất thành công");
    }
}