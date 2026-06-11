package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.AuthRequest.*;
import com.tutornet.tutor_net.dto.response.AuthResponse;
import com.tutornet.tutor_net.dto.response.TokenResponse;
import com.tutornet.tutor_net.dto.response.UserResponse;

public interface AuthService {
    AuthResponse registerClient(RegisterRequest request);
    AuthResponse loginClient(LoginRequest request);
    AuthResponse loginAdmin(LoginRequest request);
    UserResponse getCurrentUser(String email);
    TokenResponse refreshToken(
            RefreshTokenRequest request
    );
    void verifyEmailToken(String token);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void logout(String token);
}
