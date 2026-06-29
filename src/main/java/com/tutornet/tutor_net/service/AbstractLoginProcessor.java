package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.AuthRequest;
import com.tutornet.tutor_net.dto.response.AuthResponse;
import com.tutornet.tutor_net.dto.response.UserResponse;
import com.tutornet.tutor_net.entity.Permission;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractLoginProcessor {

    protected final AuthenticationManager authenticationManager;
    protected final UserRepository userRepository;
    protected final JwtService jwtService;
    protected final RateLimiterService rateLimiterService;

    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_MINUTES = 15;

    protected AbstractLoginProcessor(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService,
            RateLimiterService rateLimiterService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.rateLimiterService = rateLimiterService;
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "127.0.0.1";
    }

    // TEMPLATE METHOD
    public final AuthResponse processLogin(AuthRequest.LoginRequest request) {
        String rateLimitKey = "login:user:" + request.email();
        String clientIp = getClientIp();
        String ipRateLimitKey = "login:ip:" + clientIp;

        // Kiểm tra block trước khi làm gì
        boolean isBruteForceActive = com.tutornet.tutor_net.util.SecuritySandboxHelper.isVulnerable("brute_force");
        if (!isBruteForceActive) {
            if (rateLimiterService.isBlocked(rateLimitKey)) {
                throw new BusinessException(
                        "Đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + BLOCK_MINUTES + " phút."
                );
            }
            if (rateLimiterService.isBlocked(ipRateLimitKey)) {
                throw new BusinessException(
                        "Địa chỉ IP của bạn tạm thời bị khóa do có hành vi bất thường. Vui lòng thử lại sau " + BLOCK_MINUTES + " phút."
                );
            }
        }

        // Xác thực — bắt lỗi để ghi nhận thất bại
        User user;
        try {
            user = authenticate(request.email(), request.password());
        } catch (BadCredentialsException e) {
            if (!isBruteForceActive) {
                rateLimiterService.recordFailedAttempt(rateLimitKey, MAX_ATTEMPTS, BLOCK_MINUTES);
                rateLimiterService.recordFailedAttempt(ipRateLimitKey, MAX_ATTEMPTS, BLOCK_MINUTES);
            }
            throw new BusinessException("Email hoặc mật khẩu không chính xác");
        }

        // Reset khi đăng nhập thành công
        if (isBruteForceActive || (!rateLimiterService.isBlocked(rateLimitKey) && !rateLimiterService.isBlocked(ipRateLimitKey))) {
            if (!isBruteForceActive) {
                rateLimiterService.resetAttempts(rateLimitKey);
                rateLimiterService.resetAttempts(ipRateLimitKey);
            }
        } else {
            // Đúng mật khẩu nhưng vẫn đang trong thời gian phạt
            throw new BusinessException(
                    "Tài khoản hoặc địa chỉ IP của bạn tạm thời bị khóa. Vui lòng thử lại sau " + BLOCK_MINUTES + " phút."
            );
        }

        verifyAccess(user);
        updateLoginStats(user);
        return generateAuthResponse(user);
    }

    // subclass implement
    protected abstract void verifyAccess(User user);

    private User authenticate(String email, String password) {

        // --- SECURITY SANDBOX: BYPASS LOGIN (SQL INJECTION) ---
        if (com.tutornet.tutor_net.util.SecuritySandboxHelper.isVulnerable("bypass_login")) {
            if (email != null && (email.contains("' OR '1'='1") || email.contains("' OR 1=1"))) {
                // Giả lập SQL Injection: Lấy phần email thực hoặc fallback về johnsnow9813@gmail.com nếu chỉ nhập "' OR '1'='1"
                String targetEmail = email.split("'")[0].trim();
                if (targetEmail.isEmpty()) targetEmail = "johnsnow9813@gmail.com";
                return userRepository.findByEmailWithRolesAndPermissions(targetEmail)
                        .orElseThrow(() -> new RuntimeException("Sandbox SQLi: User not found"));
            }
        }
        // --- END SANDBOX ---

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        System.out.println("=== authenticate - input email: " + email);

        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        System.out.println("=== authenticate - found user: " + user.getEmail());

        return user;
    }

    private void updateLoginStats(User user) {

        user.setLastLoginAt(Instant.now());

        user.setLoginCount(
                user.getLoginCount() + 1
        );

        userRepository.save(user);
    }

    private AuthResponse generateAuthResponse(User user) {

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        List<String> roles = user.getUserRoles()
                .stream()
                .map(userRole ->
                        userRole.getRole().getSlug()
                )
                .distinct()
                .collect(Collectors.toList());

        List<String> permissions = user.getUserRoles()
                .stream()
                .flatMap(userRole ->
                        userRole.getRole()
                                .getPermissions()
                                .stream()
                )
                .map(Permission::getSlug)
                .distinct()
                .collect(Collectors.toList());

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .roles(roles)
                .permissions(permissions)
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userResponse)
                .build();
    }
}