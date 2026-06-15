package com.tutornet.tutor_net.service;

public interface RateLimiterService {
    boolean isBlocked(String key);
    void recordFailedAttempt(String key, int maxAttempts, int blockDurationMinutes);
    void resetAttempts(String key);
}
