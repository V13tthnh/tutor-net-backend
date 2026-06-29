package com.tutornet.tutor_net.controller.sandbox;

import com.tutornet.tutor_net.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/demo/access")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AccessControlVulnController {

    private final JdbcTemplate jdbc;
    private final JwtService jwtService;

    // Danh sách data mẫu nhạy cảm giả lập để phục vụ demo
    private static final Map<Long, Map<String, Object>> MOCK_USERS_DB = new HashMap<>();
    static {
        MOCK_USERS_DB.put(1L, Map.of("id", 1L, "email", "admin@tutornet.vn", "fullName", "Super Admin", "role", "ADMIN", "phone", "0900000001", "address", "HCM City", "bankAccount", "9999-8888-7777"));
        MOCK_USERS_DB.put(2L, Map.of("id", 2L, "email", "alice@tutornet.vn", "fullName", "Alice Nguyen", "role", "STUDENT", "phone", "0912345678", "address", "123 Lê Lợi, Q1", "bankAccount", "1111-2222-3333"));
        MOCK_USERS_DB.put(3L, Map.of("id", 3L, "email", "bob@tutornet.vn", "fullName", "Bob Tran", "role", "TUTOR", "phone", "0923456789", "address", "456 Nguyễn Huệ, Q1", "bankAccount", "1234-5678-9012"));
        MOCK_USERS_DB.put(5L, Map.of("id", 5L, "email", "charlie@tutornet.vn", "fullName", "Charlie Le", "role", "STUDENT", "phone", "0934567890", "address", "789 Phạm Ngũ Lão, Q1", "bankAccount", "4444-5555-6666"));
    }

    private static final Map<String, Object> SYSTEM_INFO = Map.of(
            "version", "2.1.0",
            "dbHost", "postgres://localhost:5432/tutornet",
            "nodeEnv", "production",
            "jwtSecret", "tutornet-super-secret-key-2024"
    );

    private static final Map<String, Object> ADMIN_DASHBOARD = Map.of(
            "totalUsers", 1247,
            "revenue", 182500000,
            "activeContracts", 89
    );

    // =================================================================
    //  1. MISSING AUTHENTICATION
    // =================================================================

    @GetMapping("/users/vulnerable")
    public ResponseEntity<?> getUsersVulnerable() {
        log.warn("[DEMO-SANDBOX] Missing Auth Vulnerable: Serving sensitive user list without verification");
        return ResponseEntity.ok(MOCK_USERS_DB.values());
    }

    @GetMapping("/users/safe")
    public ResponseEntity<?> getUsersSafe(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized", "message", "Thiếu Authorization header!"));
        }
        String token = authHeader.substring(7);
        if (!jwtService.isValidAccessToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized", "message", "Token không hợp lệ!"));
        }
        return ResponseEntity.ok(MOCK_USERS_DB.values());
    }

    @GetMapping("/system-info/vulnerable")
    public ResponseEntity<?> getSystemInfoVulnerable() {
        log.warn("[DEMO-SANDBOX] Missing Auth Vulnerable: Serving system config without verification");
        return ResponseEntity.ok(SYSTEM_INFO);
    }

    @GetMapping("/system-info/safe")
    public ResponseEntity<?> getSystemInfoSafe(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized", "message", "Thiếu Authorization header!"));
        }
        String token = authHeader.substring(7);
        if (!jwtService.isValidAccessToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized", "message", "Token không hợp lệ!"));
        }
        
        // Trích xuất email & role kiểm tra admin
        String email = jwtService.extractUsername(token);
        // Ở đây ta đơn giản giả lập kiểm tra tài khoản admin từ email
        if (!email.contains("admin")) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden", "message", "Bạn không có quyền truy cập thông tin hệ thống!"));
        }
        
        return ResponseEntity.ok(SYSTEM_INFO);
    }

    // =================================================================
    //  2. BOLA (Broken Object Level Authorization)
    // =================================================================

    @GetMapping("/users/{id}/vulnerable")
    public ResponseEntity<?> getUserBolaVulnerable(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        log.warn("[DEMO-SANDBOX] BOLA Vulnerable: Serving user #{} data without ownership check", id);
        // BOLA chỉ check có token hợp lệ hay không, không kiểm tra quyền sở hữu ID
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String token = authHeader.substring(7);
        if (!jwtService.isValidAccessToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Map<String, Object> userData = MOCK_USERS_DB.get(id);
        if (userData == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        return ResponseEntity.ok(userData);
    }

    @GetMapping("/users/{id}/safe")
    public ResponseEntity<?> getUserBolaSafe(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String token = authHeader.substring(7);
        if (!jwtService.isValidAccessToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = jwtService.extractUsername(token);
        Map<String, Object> userData = MOCK_USERS_DB.get(id);
        if (userData == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        // Kiểm tra quyền sở hữu (Ownership check)
        String ownerEmail = (String) userData.get("email");
        if (!email.equals(ownerEmail) && !email.contains("admin")) {
            log.warn("[DEMO-SANDBOX] BOLA Safe Blocked: user {} tried to access user #{}", email, id);
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Forbidden",
                    "message", "Từ chối: Bạn không có quyền truy cập dữ liệu của người dùng khác!"
            ));
        }

        return ResponseEntity.ok(userData);
    }

    // =================================================================
    //  3. BYPASS AUTH
    // =================================================================

    @GetMapping("/admin-dashboard/vulnerable")
    public ResponseEntity<?> getAdminDashboardVulnerable(
            @RequestParam(value = "admin", required = false) String adminParam,
            @RequestParam(value = "role", required = false) String roleParam,
            @RequestHeader(value = "X-Admin", required = false) String adminHeader
    ) {
        // Logic bị lỗi: tin tưởng client-controlled params/headers
        boolean bypass = "true".equals(adminParam) || "1".equals(adminHeader) || "ADMIN".equals(roleParam);
        
        if (bypass) {
            log.warn("[DEMO-SANDBOX] Bypass Auth Vulnerable: Skiped authorization via query/header parameters");
            return ResponseEntity.ok(ADMIN_DASHBOARD);
        }

        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized", "message", "Yêu cầu quyền Admin"));
    }

    @GetMapping("/admin-dashboard/safe")
    public ResponseEntity<?> getAdminDashboardSafe(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String token = authHeader.substring(7);
        if (!jwtService.isValidAccessToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = jwtService.extractUsername(token);
        // Chỉ chấp nhận admin thực sự
        if (!email.contains("admin")) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden", "message", "Access denied: Admin role required"));
        }

        return ResponseEntity.ok(ADMIN_DASHBOARD);
    }

    // =================================================================
    //  4. JWT BRUTE FORCE & WEAK SECRET
    // =================================================================

    @PostMapping("/jwt/brute")
    public ResponseEntity<?> bruteCheck(@RequestParam("secret") String secret) {
        // Thư viện brute force gọi thử, nếu secret là "secret" (key yếu của server) thì trả về tìm thấy
        boolean found = "secret".equals(secret);
        return ResponseEntity.ok(Map.of("success", found));
    }

    @PostMapping("/jwt/verify/vulnerable")
    public ResponseEntity<?> verifyVulnerable(@RequestParam("token") String token) {
        log.warn("[DEMO-SANDBOX] JWT Verify Vulnerable: Validating signature with WEAK key 'secret'");
        try {
            // Sử dụng key yếu 'secret' để phân giải token giả mạo của attacker
            SecretKey key = Keys.hmacShaKeyFor("secret".getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return ResponseEntity.ok(Map.of(
                    "verified", true,
                    "subject", claims.getSubject(),
                    "role", claims.get("role", String.class),
                    "issuedAt", claims.getIssuedAt(),
                    "message", "Chữ ký HỢP LỆ (Bằng khóa yếu 'secret')! Đăng nhập thành công với quyền Admin giả mạo.",
                    "data", ADMIN_DASHBOARD
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("verified", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/jwt/verify/safe")
    public ResponseEntity<?> verifySafe(@RequestParam("token") String token) {
        log.info("[DEMO-SANDBOX] JWT Verify Safe: Validating signature with strong system key");
        try {
            // Safe: sử dụng class verify chuẩn của hệ thống (sẽ throw error vì signature không khớp)
            if (!jwtService.isValidAccessToken(token)) {
                return ResponseEntity.status(401).body(Map.of(
                        "verified", false,
                        "error", "Signature verification failed",
                        "message", "Chữ ký KHÔNG hợp lệ! Khóa bí mật hệ thống đã bác bỏ token giả mạo này."
                ));
            }
            String email = jwtService.extractUsername(token);
            return ResponseEntity.ok(Map.of(
                    "verified", true,
                    "subject", email,
                    "message", "Token hợp lệ."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "verified", false,
                    "error", e.getMessage(),
                    "message", "Chữ ký KHÔNG hợp lệ! Khóa bí mật hệ thống đã bác bỏ token giả mạo này."
            ));
        }
    }
}
