package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.entity.User;

public interface JwtService {

    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    String extractUsername(String token);

    boolean isValid(String token);

    boolean isValidAccessToken(String token);   // ← thêm
    boolean isValidRefreshToken(String token);
}
