package com.tutornet.tutor_net.controller.sandbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chương 5 — AUTHENTICATION VULNERABILITIES
 *
 * TC-CH5-01  POST /api/demo/auth/login/no-ratelimit      Brute force — không có rate limiting
 * TC-CH5-02  POST /api/demo/auth/login/with-ratelimit    Fix: rate limiting 5 lần / 15 phút
 * TC-CH5-03  POST /api/demo/auth/credential-stuffing     Credential stuffing simulation
 * TC-CH5-04  GET  /api/demo/auth/weak-passwords          Liệt kê users dùng weak password
 * TC-CH5-05  POST /api/demo/auth/reset-attempts          Reset bộ đếm (cho demo)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/demo/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthVulnController {

    private final JdbcTemplate jdbc;

    // In-memory attempt tracker: email -> [count, firstAttemptTime]
    private final ConcurrentHashMap<String, AtomicInteger> attemptCount  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant>       firstAttempt  = new ConcurrentHashMap<>();

    private static final int    MAX_ATTEMPTS   = 5;
    private static final long   WINDOW_SECONDS = 15 * 60; // 15 phút

    // ----------------------------------------------------------------
    //  TC-CH5-01 · VULNERABLE — không có rate limiting
    //
    //  Attacker gửi hàng ngàn request → brute force password
    //  Thử: gọi liên tục với password khác nhau, không bị block
    // ----------------------------------------------------------------
    @PostMapping("/login/no-ratelimit")
    public ResponseEntity<?> loginNoRateLimit(@RequestBody Map<String, String> body) {
        String email    = body.getOrDefault("email", "");
        String password = body.getOrDefault("password", "");

        // Không đếm số lần thử, không block, không log bất thường
        boolean success = checkCredentials(email, password);

        log.warn("[DEMO-CH5] Brute-forceable login attempt — email={}, success={}", email, success);

        return ResponseEntity.ok(Map.of(
                "endpoint",     "NO RATE LIMIT (VULNERABLE)",
                "loginSuccess", success,
                "message",      success ? "Đăng nhập thành công!" : "Sai mật khẩu. Thử lại.",
                "attemptsBlocked", false,
                "warning",      "Endpoint này KHÔNG có rate limiting — có thể brute force vô hạn lần."
        ));
    }

    // ----------------------------------------------------------------
    //  TC-CH5-02 · SAFE — rate limiting 5 lần / 15 phút
    //
    //  Fix: đếm số lần thử theo email, block sau MAX_ATTEMPTS lần
    //  Production: dùng Bucket4j / Redis để distributed rate limiting
    // ----------------------------------------------------------------
    @PostMapping("/login/with-ratelimit")
    public ResponseEntity<?> loginWithRateLimit(@RequestBody Map<String, String> body) {
        String email    = body.getOrDefault("email", "");
        String password = body.getOrDefault("password", "");

        // Kiểm tra rate limit
        if (isRateLimited(email)) {
            int remaining = remainingLockSeconds(email);
            log.warn("[DEMO-CH5] Rate limit triggered for email={}", email);
            return ResponseEntity.status(429).body(Map.of(
                    "endpoint",         "WITH RATE LIMIT (SAFE)",
                    "loginSuccess",     false,
                    "rateLimited",      true,
                    "lockRemainingSec", remaining,
                    "message",          "Quá nhiều lần thử sai. Vui lòng thử lại sau "
                            + (remaining / 60) + " phút " + (remaining % 60) + " giây."
            ));
        }

        boolean success = checkCredentials(email, password);

        if (!success) {
            // Tăng attempt counter
            recordFailedAttempt(email);
            int attemptsLeft = MAX_ATTEMPTS - attemptCount.getOrDefault(email, new AtomicInteger(0)).get();
            return ResponseEntity.status(401).body(Map.of(
                    "endpoint",     "WITH RATE LIMIT (SAFE)",
                    "loginSuccess", false,
                    "attemptsLeft", Math.max(0, attemptsLeft),
                    "message",      "Sai mật khẩu. Còn " + Math.max(0, attemptsLeft) + " lần thử."
            ));
        }

        // Đăng nhập thành công → reset counter
        attemptCount.remove(email);
        firstAttempt.remove(email);

        return ResponseEntity.ok(Map.of(
                "endpoint",     "WITH RATE LIMIT (SAFE)",
                "loginSuccess", true,
                "message",      "Đăng nhập thành công!"
        ));
    }

    // ----------------------------------------------------------------
    //  TC-CH5-03 · Credential Stuffing simulation
    //
    //  Attacker dùng danh sách email:password bị leak từ nơi khác
    //  để thử đăng nhập vào TutorNet.
    //
    //  Body: { "credentials": [{"email":"a@b.com","password":"123456"}, ...] }
    // ----------------------------------------------------------------
    @PostMapping("/credential-stuffing")
    public ResponseEntity<?> credentialStuffing(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> credentials =
                (List<Map<String, String>>) body.getOrDefault("credentials", List.of());

        List<Map<String, Object>> results = new ArrayList<>();
        int hitCount = 0;

        for (Map<String, String> cred : credentials) {
            String email    = cred.getOrDefault("email", "");
            String password = cred.getOrDefault("password", "");

            // Simulate: endpoint không có rate limiting per-IP hoặc per-account
            boolean success = checkCredentials(email, password);
            if (success) hitCount++;

            results.add(Map.of(
                    "email",   email,
                    "success", success,
                    "status",  success ? " HIT — account bị compromise!" : " miss"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "endpoint",        "CREDENTIAL STUFFING (VULNERABLE)",
                "totalTried",      credentials.size(),
                "hits",            hitCount,
                "successRate",     credentials.isEmpty() ? "0%" :
                        String.format("%.1f%%", (hitCount * 100.0 / credentials.size())),
                "results",         results,
                "fix",             "Fix: rate limiting per-IP + CAPTCHA + breached password check (HaveIBeenPwned API)"
        ));
    }

    // ----------------------------------------------------------------
    //  TC-CH5-04 · Weak password audit
    //  Liệt kê accounts có password hash khớp với common passwords
    // ----------------------------------------------------------------
    @GetMapping("/weak-passwords")
    public ResponseEntity<?> weakPasswordAudit() {
        // Danh sách bcrypt hash của top 5 weak passwords (demo only)
        List<String> weakPasswords = List.of("123456", "password", "123456789", "qwerty", "abc123");

        List<Map<String, Object>> vulnerableAccounts = new ArrayList<>();

        try {
            List<Map<String, Object>> users = jdbc.queryForList(
                    "SELECT id, email, password_hash FROM users LIMIT 100");

            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

            for (Map<String, Object> user : users) {
                String hash = (String) user.get("password_hash");
                for (String weak : weakPasswords) {
                    try {
                        if (encoder.matches(weak, hash)) {
                            vulnerableAccounts.add(Map.of(
                                    "email",          user.get("email"),
                                    "weakPassword",   weak,
                                    "risk",           "HIGH"
                            ));
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }

        return ResponseEntity.ok(Map.of(
                "endpoint",           "WEAK PASSWORD AUDIT",
                "vulnerableAccounts", vulnerableAccounts,
                "totalVulnerable",    vulnerableAccounts.size(),
                "checkedPasswords",   weakPasswords,
                "fix",                "Enforce strong password policy + check against HaveIBeenPwned database"
        ));
    }

    // ----------------------------------------------------------------
    //  TC-CH5-05 · Reset attempt counter (cho demo)
    // ----------------------------------------------------------------
    @PostMapping("/reset-attempts")
    public ResponseEntity<?> resetAttempts(@RequestBody(required = false) Map<String, String> body) {
        String email = body != null ? body.get("email") : null;
        if (email != null) {
            attemptCount.remove(email);
            firstAttempt.remove(email);
            return ResponseEntity.ok(Map.of("message", "Đã reset attempt counter cho: " + email));
        } else {
            attemptCount.clear();
            firstAttempt.clear();
            return ResponseEntity.ok(Map.of("message", "Đã reset tất cả attempt counters."));
        }
    }

    // ----------------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------------

    private boolean checkCredentials(String email, String password) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT password_hash FROM users WHERE email = ?", email);
            if (rows.isEmpty()) return false;
            String hash = (String) rows.get(0).get("password_hash");
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            return encoder.matches(password, hash);
        } catch (Exception e) {
            return false;
        }
    }

    private void recordFailedAttempt(String email) {
        attemptCount.computeIfAbsent(email, k -> new AtomicInteger(0)).incrementAndGet();
        firstAttempt.putIfAbsent(email, Instant.now());
    }

    private boolean isRateLimited(String email) {
        AtomicInteger count = attemptCount.get(email);
        Instant first = firstAttempt.get(email);
        if (count == null || first == null) return false;

        long elapsed = Instant.now().getEpochSecond() - first.getEpochSecond();
        if (elapsed > WINDOW_SECONDS) {
            // Window hết hạn → reset
            attemptCount.remove(email);
            firstAttempt.remove(email);
            return false;
        }
        return count.get() >= MAX_ATTEMPTS;
    }

    private int remainingLockSeconds(String email) {
        Instant first = firstAttempt.get(email);
        if (first == null) return 0;
        long elapsed = Instant.now().getEpochSecond() - first.getEpochSecond();
        return (int) Math.max(0, WINDOW_SECONDS - elapsed);
    }
}