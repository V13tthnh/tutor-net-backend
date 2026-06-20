package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RateLimiterServiceImpl implements RateLimiterService {

    private final Map<String, Instant> blockedKeys = new ConcurrentHashMap<>();
    private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();

    @Override
    public boolean isBlocked(String key) {
        Instant expiry = blockedKeys.get(key);
        if (expiry == null) return false;
        if (Instant.now().isAfter(expiry)) {
            blockedKeys.remove(key);
            attemptsCache.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Ghi nhận 1 lần thất bại. Có thể tùy chỉnh số lần tối đa và thời gian phạt cho từng tính năng.
     */
    @Override
    public void recordFailedAttempt(String key, int maxAttempts, int blockDurationMinutes) {
        int attempts = attemptsCache.getOrDefault(key, 0) + 1;
        if (attempts >= maxAttempts) {
            blockedKeys.put(key, Instant.now().plus(Duration.ofMinutes(blockDurationMinutes)));
            attemptsCache.remove(key);
        } else {
            attemptsCache.put(key, attempts);
        }
    }

    /**
     * Reset lại số lần sai khi thao tác thành công
     */
    @Override
    public void resetAttempts(String key) {
        attemptsCache.remove(key);
        blockedKeys.remove(key);
    }
}
