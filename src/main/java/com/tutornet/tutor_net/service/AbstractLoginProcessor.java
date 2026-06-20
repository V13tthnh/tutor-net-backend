package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.AuthRequest;
import com.tutornet.tutor_net.dto.response.AuthResponse;
import com.tutornet.tutor_net.dto.response.UserResponse;
import com.tutornet.tutor_net.entity.Permission;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

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


    // TEMPLATE METHOD
    public final AuthResponse processLogin(AuthRequest.LoginRequest request) {
        String rateLimitKey = "login:user:" + request.email();

        // Kiểm tra block trước khi làm gì
        if (rateLimiterService.isBlocked(rateLimitKey)) {
            throw new BusinessException(
                    "Đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + BLOCK_MINUTES + " phút."
            );
        }

        // Xác thực — bắt lỗi để ghi nhận thất bại
        User user;
        try {
            user = authenticate(request.email(), request.password());
        } catch (BadCredentialsException e) {
            rateLimiterService.recordFailedAttempt(rateLimitKey, MAX_ATTEMPTS, BLOCK_MINUTES);
            throw new BusinessException("Email hoặc mật khẩu không chính xác");
        }

        // Reset khi đăng nhập thành công
        if (!rateLimiterService.isBlocked(rateLimitKey)) {
            rateLimiterService.resetAttempts(rateLimitKey);
        } else {
            // Đúng mật khẩu nhưng vẫn đang trong thời gian phạt
            throw new BusinessException(
                    "Tài khoản tạm thời bị khóa. Vui lòng thử lại sau " + BLOCK_MINUTES + " phút."
            );
        }

        verifyAccess(user);
        updateLoginStats(user);
        return generateAuthResponse(user);
    }

    // subclass implement
    protected abstract void verifyAccess(User user);

    private User authenticate(String email, String password) {

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