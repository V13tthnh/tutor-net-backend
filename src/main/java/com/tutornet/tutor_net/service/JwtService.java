package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.entity.User;

import java.time.Instant;

public interface JwtService {

    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    String extractUsername(String token);

    boolean isValid(String token);

    boolean isValidAccessToken(String token);

    boolean isValidRefreshToken(String token);

    Instant getExpirationFromToken(String token);;
}
