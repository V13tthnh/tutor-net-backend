package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.AuthRequest.*;
import com.tutornet.tutor_net.dto.response.AuthResponse;
import com.tutornet.tutor_net.dto.response.TokenResponse;
import com.tutornet.tutor_net.dto.response.UserResponse;
import com.tutornet.tutor_net.entity.*;
import com.tutornet.tutor_net.enums.UserStatus;
import com.tutornet.tutor_net.event.PasswordResetRequestedEvent;
import com.tutornet.tutor_net.event.UserRegisteredEvent;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.exception.BadRequestException;
import com.tutornet.tutor_net.repository.PasswordResetTokenRepository;
import com.tutornet.tutor_net.repository.RoleRepository;
import com.tutornet.tutor_net.repository.UserRepository;
import com.tutornet.tutor_net.repository.VerificationTokenRepository;
import com.tutornet.tutor_net.security.processor.AdminLoginProcessor;
import com.tutornet.tutor_net.security.processor.ClientLoginProcessor;
import com.tutornet.tutor_net.service.AuthService;
import com.tutornet.tutor_net.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    // Tiêm các Login Processors áp dụng Template Method
    private final ClientLoginProcessor clientLoginProcessor;
    private final AdminLoginProcessor adminLoginProcessor;

    private static final long RESET_TOKEN_EXPIRY_MINUTES = 15;

    @Override
    @Transactional
    public AuthResponse registerClient(RegisterRequest request) {

        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email này đã được sử dụng");
        }

        Role studentRole = roleRepository.findBySlug("student")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Role 'student' trong hệ thống"));

        Role tutorRole = roleRepository.findBySlug("tutor")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Role 'tutor' trong hệ thống"));

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .status(UserStatus.PENDING_VERIFICATION)
                .isVerified(false)
                .build();

        user.getUserRoles().add(UserRole.builder().user(user).role(studentRole).build());
        user.getUserRoles().add(UserRole.builder().user(user).role(tutorRole).build());

        User savedUser = userRepository.save(user);

        String tokenString = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(user);
        verificationToken.setToken(tokenString);
        verificationToken.setTokenType("email_verify");
        verificationToken.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));
        tokenRepository.save(verificationToken);

        eventPublisher.publishEvent(
                new UserRegisteredEvent(user, tokenString)
        );

        return AuthResponse.builder()
                .message("Đăng ký thành công! Vui lòng kiểm tra hộp thư email của bạn để hoàn tất xác thực")
                .build();
    }

    @Override
    @Transactional
    public void verifyEmailToken(String token) {
        VerificationToken vToken = tokenRepository.findByTokenAndTokenType(token, "email_verify")
                .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ hoặc không tồn tại"));

        if (vToken.getUsedAt() != null) {
            throw new IllegalArgumentException("Token này đã được sử dụng");
        }

        if (vToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Link xác thực đã hết hạn");
        }

        // Đánh dấu token đã dùng
        vToken.setUsedAt(Instant.now());
        tokenRepository.save(vToken);

        // Cập nhật User thành Active
        User user = vToken.getUser();
        user.setIsVerified(true);
        user.setEmailVerifiedAt(Instant.now());
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public AuthResponse loginClient(LoginRequest request) {
        // Ủy thác cho bộ xử lý đăng nhập Client
        return clientLoginProcessor.processLogin(request);
    }

    @Override
    @Transactional
    public AuthResponse loginAdmin(LoginRequest request) {
        // Ủy thác cho bộ xử lý đăng nhập Admin
        return adminLoginProcessor.processLogin(request);
    }

    @Override
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getSlug())
                .distinct()
                .toList();

        List<String> permissions = user.getUserRoles().stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(Permission::getSlug)
                .distinct()
                .toList();

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtService.isValidRefreshToken(refreshToken)) {
            throw new RuntimeException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String email = jwtService.extractUsername(refreshToken);
        System.out.println("=== refreshToken - extracted email: " + email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println("=== refreshToken - found user: " + user.getEmail());

        return TokenResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Tìm user — nếu không thấy vẫn trả về bình thường (bảo mật)
        Optional<User> userOpt = userRepository.findByEmail(request.email());
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        // Tạo token UUID
        String rawToken = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(rawToken)
                .expiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Gửi email
        eventPublisher.publishEvent(
                new PasswordResetRequestedEvent(user.getEmail(), user.getFullName(), rawToken)
        );
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.token())
                .orElseThrow(() -> new BadRequestException("Token không hợp lệ hoặc đã được sử dụng"));

        if (resetToken.getUsed()) {
            throw new BadRequestException("Token đã được sử dụng");
        }
        if (resetToken.isExpired()) {
            throw new BadRequestException("Token đã hết hạn. Vui lòng yêu cầu lại");
        }

        // Cập nhật mật khẩu
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Vô hiệu hoá tất cả token reset của user này
        passwordResetTokenRepository.invalidateAllTokensForUser(user);
    }

    @Override
    public void logout(String token) {
        // JWT là stateless, việc xóa token chủ yếu xử lý ở Frontend.
        // Nếu cần làm tầng bảo mật cao, bạn có thể triển khai lưu token vào Redis Blacklist tại đây.
    }
}