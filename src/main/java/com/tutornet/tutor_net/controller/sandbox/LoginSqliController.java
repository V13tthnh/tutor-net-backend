package com.tutornet.tutor_net.controller.sandbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Chương 4 — SQL INJECTION: Bypass Login
 *
 * TC-CH4-01  POST /api/demo/sqli/login/vulnerable   Bypass login bằng SQLi
 * TC-CH4-02  POST /api/demo/sqli/login/safe         Fix: parameterized + bcrypt check
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/demo/sqli")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LoginSqliController {

    private final JdbcTemplate jdbc;

    // ----------------------------------------------------------------
    //  TC-CH4-01 · VULNERABLE — bypass login
    //
    //  Payload email:    ' OR '1'='1' --
    //  Payload email:    admin@tutornet.com' --
    //  Payload password: anything
    // ----------------------------------------------------------------
    @PostMapping("/login/vulnerable")
    public ResponseEntity<?> vulnerableLogin(@RequestBody Map<String, String> body) {
        String email    = body.getOrDefault("email", "");
        String password = body.getOrDefault("password", "");

        // ❌ String concat trực tiếp — attacker bypass hoàn toàn
        String sql = "SELECT id, email, full_name, password_hash FROM users "
                + "WHERE email = '" + email + "' "
                + "AND password_hash = '" + password + "'";

        log.warn("[DEMO-CH4] UNSAFE login query: {}", sql);

        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            if (!rows.isEmpty()) {
                Map<String, Object> user = rows.get(0);
                return ResponseEntity.ok(Map.of(
                        "endpoint",      "VULNERABLE LOGIN",
                        "sql",           sql,
                        "loginSuccess",  true,
                        "loggedInAs",    user.get("email"),
                        "userId",        user.get("id"),
                        "warning",       "Đăng nhập thành công mà KHÔNG cần đúng password!"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "endpoint",     "VULNERABLE LOGIN",
                        "sql",          sql,
                        "loginSuccess", false
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "endpoint", "VULNERABLE LOGIN",
                    "sql",      sql,
                    "error",    e.getMessage()
            ));
        }
    }

    // ----------------------------------------------------------------
    //  TC-CH4-02 · SAFE — parameterized + BCrypt verify
    //
    //  Fix:  1. Dùng parameterized query để lấy user theo email
    //        2. Verify password bằng BCryptPasswordEncoder — không so sánh plain text
    // ----------------------------------------------------------------
    @PostMapping("/login/safe")
    public ResponseEntity<?> safeLogin(@RequestBody Map<String, String> body) {
        String email    = body.getOrDefault("email", "");
        String password = body.getOrDefault("password", "");

        // ✅ Parameterized — email không thể thoát ra ngoài string literal
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, email, full_name, password_hash FROM users WHERE email = ?",
                email
        );

        if (rows.isEmpty()) {
            // Không tiết lộ "email không tồn tại" — tránh user enumeration
            return ResponseEntity.status(401).body(Map.of(
                    "endpoint",     "SAFE LOGIN",
                    "loginSuccess", false,
                    "message",      "Email hoặc mật khẩu không đúng."
            ));
        }

        Map<String, Object> user = rows.get(0);
        String storedHash = (String) user.get("password_hash");

        // ✅ BCrypt verify — không so sánh plain text
        boolean passwordMatch = verifyBcrypt(password, storedHash);

        if (passwordMatch) {
            return ResponseEntity.ok(Map.of(
                    "endpoint",     "SAFE LOGIN",
                    "loginSuccess", true,
                    "loggedInAs",   user.get("email")
            ));
        } else {
            return ResponseEntity.status(401).body(Map.of(
                    "endpoint",     "SAFE LOGIN",
                    "loginSuccess", false,
                    "message",      "Email hoặc mật khẩu không đúng."
            ));
        }
    }

    /**
     * Simulate BCrypt verify (thực tế dùng BCryptPasswordEncoder.matches())
     * Trong production: inject BCryptPasswordEncoder và gọi .matches(raw, stored)
     */
    private boolean verifyBcrypt(String rawPassword, String storedHash) {
        try {
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            return encoder.matches(rawPassword, storedHash);
        } catch (Exception e) {
            return false;
        }
    }
}