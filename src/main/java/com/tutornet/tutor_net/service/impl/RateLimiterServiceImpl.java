package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RateLimiterServiceImpl implements RateLimiterService {
    // Lưu thời gian hết hạn khóa của từng Key
    private final Map<String, LocalDateTime> blockedKeys = new ConcurrentHashMap<>();
    // Lưu số lần thử thất bại của từng Key
    private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();

    /**
     * Kiểm tra xem Key này có đang bị khóa không
     */
    public boolean isBlocked(String key) {
        if (blockedKeys.containsKey(key)) {
            if (LocalDateTime.now().isBefore(blockedKeys.get(key))) {
                return true; // Vẫn đang trong thời gian bị khóa
            } else {
                // Đã hết thời gian khóa -> Xóa khỏi danh sách chặn
                blockedKeys.remove(key);
                attemptsCache.remove(key);
            }
        }
        return false;
    }

    /**
     * Ghi nhận 1 lần thất bại. Có thể tùy chỉnh số lần tối đa và thời gian phạt cho từng tính năng.
     */
    public void recordFailedAttempt(String key, int maxAttempts, int blockDurationMinutes) {
        int attempts = attemptsCache.getOrDefault(key, 0) + 1;
        if (attempts >= maxAttempts) {
            // Vượt quá số lần cho phép -> Phạt khóa
            blockedKeys.put(key, LocalDateTime.now().plusMinutes(blockDurationMinutes));
            attemptsCache.remove(key);
        } else {
            // Chưa vượt quá -> Cập nhật số lần sai
            attemptsCache.put(key, attempts);
        }
    }

    /**
     * Reset lại số lần sai khi thao tác thành công
     */
    public void resetAttempts(String key) {
        attemptsCache.remove(key);
    }
}
