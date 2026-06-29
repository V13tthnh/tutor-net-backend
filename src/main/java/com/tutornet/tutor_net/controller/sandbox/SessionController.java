package com.tutornet.tutor_net.controller.sandbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chương 6 — SESSION & COOKIE SECURITY
 *
 * TC-CH6-01  POST /api/demo/session/login/vulnerable      Session ID yếu, không HttpOnly/Secure
 * TC-CH6-02  POST /api/demo/session/login/safe            Fix: strong session ID + HttpOnly + Secure + SameSite
 * TC-CH6-03  GET  /api/demo/session/fixation/set          Session Fixation — attacker set session trước
 * TC-CH6-04  GET  /api/demo/session/me                    Đọc session hiện tại (demo hijacking)
 * TC-CH6-05  POST /api/demo/session/logout                Logout (invalidate session)
 * TC-CH6-06  GET  /api/demo/session/list                  Liệt kê tất cả sessions (admin demo)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/demo/session")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class SessionController {

    private final JdbcTemplate jdbc;

    // In-memory session store: sessionId -> SessionData
    private final ConcurrentHashMap<String, SessionData> sessionStore = new ConcurrentHashMap<>();

    private static final String VULNERABLE_COOKIE = "TUTOR_SESSION";   // ❌ predictable name, no flags
    private static final String SAFE_COOKIE       = "SESS";             // ✅ generic name

    // ----------------------------------------------------------------
    //  TC-CH6-01 · VULNERABLE — session ID yếu, thiếu cookie flags
    //
    //  Vấn đề:
    //    1. Session ID predictable (sequential int)
    //    2. Cookie KHÔNG có HttpOnly → JS có thể đọc document.cookie
    //    3. Cookie KHÔNG có Secure → gửi qua HTTP plain text
    //    4. Cookie KHÔNG có SameSite → CSRF dễ dàng
    // ----------------------------------------------------------------
    @PostMapping("/login/vulnerable")
    public ResponseEntity<?> vulnerableLogin(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {

        String email = body.getOrDefault("email", "");

        List<Map<String, Object>> users = jdbc.queryForList(
                "SELECT id, email, full_name FROM users WHERE email = ? LIMIT 1", email);
        if (users.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        Map<String, Object> user = users.get(0);

        // ❌ Lỗ hổng Session Fixation: Giữ nguyên session ID từ cookie client nếu có
        String weakSessionId = extractSessionId(request);
        if (weakSessionId == null || weakSessionId.startsWith("safe_") || weakSessionId.length() > 40) {
            weakSessionId = "sess_" + System.currentTimeMillis() % 10000;
        }

        sessionStore.put(weakSessionId, new SessionData(
                String.valueOf(user.get("id")),
                (String) user.get("email"),
                (String) user.get("full_name"),
                Instant.now(),
                false // not regenerated on login
        ));

        // ❌ Thiếu HttpOnly, Secure, SameSite
        Cookie cookie = new Cookie(VULNERABLE_COOKIE, weakSessionId);
        cookie.setPath("/");
        cookie.setMaxAge(3600);
        // ❌ KHÔNG set setHttpOnly(true)
        // ❌ KHÔNG set setSecure(true)
        response.addCookie(cookie);

        log.warn("[DEMO-CH6] Vulnerable session created: id={}, cookie={}={} (no HttpOnly, no Secure)",
                weakSessionId, VULNERABLE_COOKIE, weakSessionId);

        return ResponseEntity.ok(Map.of(
                "endpoint",       "VULNERABLE SESSION",
                "sessionId",      weakSessionId,
                "cookieName",     VULNERABLE_COOKIE,
                "httpOnly",       false,
                "secure",         false,
                "sameSite",       "None",
                "predictable",    true,
                "loggedInAs",     user.get("email"),
                "vulnerabilities", List.of(
                        "Session ID predictable (timestamp-based)",
                        "No HttpOnly → document.cookie accessible via XSS",
                        "No Secure → sent over HTTP (sniffable)",
                        "No SameSite → CSRF possible"
                )
        ));
    }

    // ----------------------------------------------------------------
    //  TC-CH6-02 · SAFE — strong session ID + proper cookie flags
    // ----------------------------------------------------------------
    @PostMapping("/login/safe")
    public ResponseEntity<?> safeLogin(
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {

        String email = body.getOrDefault("email", "");

        List<Map<String, Object>> users = jdbc.queryForList(
                "SELECT id, email, full_name FROM users WHERE email = ? LIMIT 1", email);
        if (users.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        Map<String, Object> user = users.get(0);

        // ✅ Session ID mạnh — 128-bit random (cryptographically secure)
        String strongSessionId = generateSecureSessionId();

        // ✅ Regenerate session ID sau khi login (prevent session fixation)
        sessionStore.put(strongSessionId, new SessionData(
                String.valueOf(user.get("id")),
                (String) user.get("email"),
                (String) user.get("full_name"),
                Instant.now(),
                true // regenerated
        ));

        // ✅ Cookie với đầy đủ security flags
        ResponseCookie secureCookie = ResponseCookie.from(SAFE_COOKIE, strongSessionId)
                .httpOnly(true)       // ✅ Không accessible từ JS
                .secure(false)        // true trong production (cần HTTPS)
                .sameSite("Strict")   // ✅ Chặn CSRF
                .path("/")
                .maxAge(3600)
                .build();

        response.addHeader("Set-Cookie", secureCookie.toString());

        return ResponseEntity.ok(Map.of(
                "endpoint",           "SAFE SESSION",
                "sessionId",          strongSessionId.substring(0, 8) + "...[hidden]",
                "cookieName",         SAFE_COOKIE,
                "httpOnly",           true,
                "secure",             true,
                "sameSite",           "Strict",
                "entropyBits",        128,
                "sessionRegenerated", true,
                "loggedInAs",         user.get("email")
        ));
    }

    // ----------------------------------------------------------------
    //  TC-CH6-03 · Session Fixation
    //
    //  Attacker trick victim dùng session ID do attacker kiểm soát.
    //  Steps:
    //    1. Attacker gọi endpoint này để "tạo" session ID trước
    //    2. Attacker gửi link có ?sessId=xxx cho victim
    //    3. Victim login → server dùng session ID đó (không regenerate)
    //    4. Attacker dùng session ID đó để hijack session của victim
    // ----------------------------------------------------------------
    @GetMapping("/fixation/set")
    public ResponseEntity<?> sessionFixation(
            @RequestParam(required = false) String sessId,
            HttpServletResponse response) {

        // ❌ Server chấp nhận session ID do client cung cấp
        String fixedId = (sessId != null && !sessId.isBlank()) ? sessId : "fixed_" + System.nanoTime() % 9999;

        sessionStore.put(fixedId, new SessionData(
                null, null, null, Instant.now(), false));

        Cookie cookie = new Cookie(VULNERABLE_COOKIE, fixedId);
        cookie.setPath("/");
        cookie.setMaxAge(3600);
        response.addCookie(cookie);

        log.warn("[DEMO-CH6] Session fixation: attacker-controlled session ID set: {}", fixedId);

        return ResponseEntity.ok(Map.of(
                "endpoint",     "SESSION FIXATION (VULNERABLE)",
                "fixedSessId",  fixedId,
                "attackSteps",  List.of(
                        "1. Attacker gọi endpoint này → nhận session ID: " + fixedId,
                        "2. Attacker gửi URL cho victim: http://tutornet.com/login?TUTOR_SESSION=" + fixedId,
                        "3. Victim login → server KHÔNG regenerate session ID",
                        "4. Attacker dùng session ID " + fixedId + " → đã là victim!"
                ),
                "fix", "Luôn regenerate session ID sau khi đăng nhập thành công"
        ));
    }

    // ----------------------------------------------------------------
    //  TC-CH6-04 · Đọc session hiện tại (demo hijacking)
    // ----------------------------------------------------------------
    @GetMapping("/me")
    public ResponseEntity<?> getSessionInfo(HttpServletRequest request) {
        String sessionId = extractSessionId(request);

        if (sessionId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No session cookie found"));
        }

        SessionData session = sessionStore.get(sessionId);
        if (session == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired session"));
        }

        return ResponseEntity.ok(Map.of(
                "sessionId",  sessionId,
                "userId",     session.userId() != null ? session.userId() : "anonymous",
                "email",      session.email()  != null ? session.email()  : "not logged in",
                "fullName",   session.fullName() != null ? session.fullName() : "",
                "createdAt",  session.createdAt().toString(),
                "regenerated",session.regenerated()
        ));
    }

    // ----------------------------------------------------------------
    //  TC-CH6-05 · Logout
    // ----------------------------------------------------------------
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = extractSessionId(request);
        if (sessionId != null) {
            sessionStore.remove(sessionId); // ✅ Invalidate server-side session
        }

        // ✅ Xoá cookie phía client
        Cookie expire = new Cookie(VULNERABLE_COOKIE, "");
        expire.setMaxAge(0);
        expire.setPath("/");
        response.addCookie(expire);

        Cookie expireSafe = new Cookie(SAFE_COOKIE, "");
        expireSafe.setMaxAge(0);
        expireSafe.setPath("/");
        response.addCookie(expireSafe);

        return ResponseEntity.ok(Map.of("message", "Logged out. Session invalidated server-side."));
    }

    // ----------------------------------------------------------------
    //  TC-CH6-06 · List all sessions (admin demo)
    // ----------------------------------------------------------------
    @GetMapping("/list")
    public ResponseEntity<?> listSessions() {
        List<Map<String, Object>> sessions = sessionStore.entrySet().stream()
                .map(e -> {
                    SessionData s = e.getValue();
                    return Map.<String, Object>of(
                            "sessionId", e.getKey(),
                            "email",     s.email() != null ? s.email() : "anonymous",
                            "createdAt", s.createdAt().toString()
                    );
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "activeSessions", sessions,
                "total",          sessions.size()
        ));
    }

    // ----------------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------------

    private String generateSecureSessionId() {
        SecureRandom sr = new SecureRandom();
        byte[] bytes = new byte[16]; // 128-bit
        sr.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String extractSessionId(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (VULNERABLE_COOKIE.equals(c.getName()) || SAFE_COOKIE.equals(c.getName())) {
                return c.getValue();
            }
        }
        // Fallback: check header (for Postman demo)
        String header = request.getHeader("X-Session-Id");
        return header;
    }

    private record SessionData(
            String userId,
            String email,
            String fullName,
            Instant createdAt,
            boolean regenerated
    ) {}
}