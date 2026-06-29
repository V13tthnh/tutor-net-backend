package com.tutornet.tutor_net.controller.sandbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/demo/sqli")
public class SqliUnionController {
    @PersistenceContext
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Tìm gia sư theo tên — BỊ Dính UNION SQLi.
     * <p>
     * Thử payload:
     * GET /api/demo/sqli/vulnerable?name=' UNION SELECT id,email,password_hash,full_name FROM users--
     * <p>
     * Query gốc:
     * SELECT id, full_name, headline, bio FROM tutor_profiles tp
     * JOIN users u ON tp.user_id = u.id
     * WHERE u.full_name LIKE '%<INPUT>%'
     * <p>
     * Sau khi inject, DB sẽ trả về password_hash của toàn bộ users!
     */
    @GetMapping("/vulnerable")
    public ResponseEntity<?> vulnerable(@RequestParam(defaultValue = "") String name) {
        if (!com.tutornet.tutor_net.util.SecuritySandboxHelper.isVulnerable("union_sqli")) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "error", "Lỗi bảo mật: UNION-based SQLi Sandbox hiện đang bị tắt trên Server!"
            ));
        }

        String sql = """
                SELECT id, full_name, email, phone, avatar_url
                FROM users
                WHERE full_name ILIKE '%""" + name + "%'";

        try {
            // Sử dụng JdbcTemplate để hỗ trợ Multiple Statements (cho phép DROP TABLE)
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

            return ResponseEntity.ok(Map.of(
                    "sql", sql,
                    "data", result,
                    "total", result.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "sql", sql,
                    "error", e.getMessage()
            ));
        }
    }

    // =========================================================
    //  ENDPOINT ĐÃ FIX — Parameterized Query / Prepared Statement
    // =========================================================

    /**
     * Tìm gia sư theo tên — ĐÃ ĐƯỢC BẢO VỆ.
     * <p>
     * Thử cùng payload:
     * GET /api/demo/sqli/safe?name=' UNION SELECT id,email,password_hash,full_name FROM users--
     * <p>
     * Kết quả: trả về [] (empty) — input được treat như literal string, không phải SQL.
     */
    @GetMapping("/safe")
    public ResponseEntity<?> safe(@RequestParam(defaultValue = "") String name) {
        String sql = """
                SELECT id, full_name, email, avatar_url
                FROM users
                WHERE full_name ILIKE :namePattern
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("namePattern", "%" + name + "%")
                .getResultList();

        List<Map<String, Object>> result = rows.stream().map(r -> Map.of(
                "id", r[0],
                "fullName", r[1] != null ? r[1] : "",
                "email", r[2] != null ? r[2] : "",
                "avatarUrl", r[3] != null ? r[3] : ""
        )).toList();

        return ResponseEntity.ok(Map.of(
                "data", result,
                "total", result.size()
        ));
    }

    // =========================================================
    //  ENDPOINT KHÔI PHỤC DỮ LIỆU KHI DROP TABLE
    // =========================================================

    @PostMapping("/restore")
    public ResponseEntity<?> restore() {
        try {
            String restoreSql = """
                    DROP TABLE IF EXISTS users CASCADE;
                    CREATE TABLE users (
                        id BIGSERIAL PRIMARY KEY,
                        email VARCHAR(255) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL,
                        full_name VARCHAR(200) NOT NULL,
                        phone VARCHAR(20),
                        avatar_url TEXT,
                        gender_type VARCHAR(50) DEFAULT 'OTHER' NOT NULL,
                        user_status VARCHAR(50) DEFAULT 'PENDING_VERIFICATION' NOT NULL,
                        is_verified BOOLEAN DEFAULT FALSE NOT NULL,
                        email_verified_at TIMESTAMP,
                        last_login_at TIMESTAMP,
                        login_count INTEGER DEFAULT 0 NOT NULL,
                        social_links JSONB DEFAULT '{}'::jsonb,
                        birth_year INTEGER,
                        hometown_address TEXT,
                        current_address TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        deleted_at TIMESTAMP
                    );
                     INSERT INTO "users" ("id", "email", "password_hash", "full_name", "phone", "avatar_url", "status", "is_verified", "email_verified_at", "last_login_at", "login_count", "created_at", "updated_at", "deleted_at", "social_links", "gender", "hometown_address", "current_address", "birth_year") VALUES (5, 'superadmin@gmail.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Hệ thống (Super Admin)', NULL, NULL, 'ACTIVE', 't', '2026-05-23 16:07:49.489135+07', '2026-06-28 13:08:22.344869+07', 80, '2026-05-23 16:07:49.489135+07', '2026-06-28 13:08:22.200484+07', NULL, NULL, 'OTHER', NULL, NULL, NULL);
                     INSERT INTO "users" ("id", "email", "password_hash", "full_name", "phone", "avatar_url", "status", "is_verified", "email_verified_at", "last_login_at", "login_count", "created_at", "updated_at", "deleted_at", "social_links", "gender", "hometown_address", "current_address", "birth_year") VALUES (6, 'admin@gmail.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Quản trị viên (Admin)', NULL, NULL, 'ACTIVE', 't', '2026-05-23 16:07:49.489135+07', '2026-06-25 18:06:10.307589+07', 88, '2026-05-23 16:07:49.489135+07', '2026-06-25 18:06:10.17117+07', NULL, NULL, 'OTHER', NULL, NULL, NULL);
                     INSERT INTO "users" ("id", "email", "password_hash", "full_name", "phone", "avatar_url", "status", "is_verified", "email_verified_at", "last_login_at", "login_count", "created_at", "updated_at", "deleted_at", "social_links", "gender", "hometown_address", "current_address", "birth_year") VALUES (28, 'johnsnow9813@gmail.com', '$2a$10$RtMMtZoDX6AhbN/2sDvcauw8CCZxPDWUClxp0SoAwyiAL.SJKt96q', 'thành đinh', '0883456789', 'http://localhost:8080/uploads/uploads/avatars/f0d166a7-6cef-42b8-bdb7-e396cd3b0c7e.jpg', 'ACTIVE', 't', '2026-06-02 19:15:01.671219+07', '2026-06-27 00:03:27.187839+07', 37, '2026-06-02 19:14:44.22107+07', '2026-06-27 19:04:07.341478+07', NULL, '{"facebook": "https://facebook.com/thanh-dinh"}', 'MALE', NULL, '567 | Phường Phước Long | Tỉnh Đồng Nai', 2003);
                     INSERT INTO "users" ("id", "email", "password_hash", "full_name", "phone", "avatar_url", "status", "is_verified", "email_verified_at", "last_login_at", "login_count", "created_at", "updated_at", "deleted_at", "social_links", "gender", "hometown_address", "current_address", "birth_year") VALUES (49, 'guilliman0511@gmail.com', '$2a$10$voI7tkcfSCfT7YTw0N6xIu5a3OGM73lXz1p/HQ1nvgrpPNBpMozn2', 'lê trung', '0877568313', '/uploads/avatars/2e448b1c-6465-47c0-b5e6-0a3ebc15abf8.jpg', 'ACTIVE', 't', '2026-06-09 19:17:28.580051+07', '2026-06-27 16:40:54.360435+07', 8, '2026-06-09 19:17:10.236917+07', '2026-06-27 19:43:00.02297+07', NULL, '{}', 'MALE', ' |  | Thành phố Hà Nội', '567 | Quận Đống Đa | Thành phố Hà Nội', 2003);
                     INSERT INTO "users" ("id", "email", "password_hash", "full_name", "phone", "avatar_url", "status", "is_verified", "email_verified_at", "last_login_at", "login_count", "created_at", "updated_at", "deleted_at", "social_links", "gender", "hometown_address", "current_address", "birth_year") VALUES (51, 'hctrinh@hcmute.edu.vn', '$2a$10$Eyn/MxwlXN6G6OoayeAKh.EEwBq5uOE33C9qu7N.h.7SFYVDt6HkO', 'Trần Mai Hoàng Khánh', NULL, NULL, 'PENDING_VERIFICATION', 'f', NULL, NULL, 0, '2026-06-27 19:34:37.098283+07', '2026-06-27 19:34:37.098283+07', NULL, '{}', 'OTHER', NULL, NULL, NULL);
                     INSERT INTO "users" ("id", "email", "password_hash", "full_name", "phone", "avatar_url", "status", "is_verified", "email_verified_at", "last_login_at", "login_count", "created_at", "updated_at", "deleted_at", "social_links", "gender", "hometown_address", "current_address", "birth_year") VALUES (31, 'thanhlklk909@gmail.com', '$2a$10$4t9s0nXhr45r92wjfUpc/ul4dLz1LzQy/qHjyQ0PUOvB2opAAPAM.', 'Nguyễn Văn A', '0333456789', 'http://localhost:8080/uploads/uploads/avatars/4fced0b4-0848-472d-b89e-ce4292c28c50.jpg', 'ACTIVE', 't', '2026-06-06 00:01:07.629192+07', '2026-06-27 19:38:13.903715+07', 30, '2026-06-05 23:59:42.262135+07', '2026-06-27 19:38:13.753448+07', NULL, '{"facebook": ""}', 'MALE', ' |  | Tỉnh Cao Bằng', '120 | Xã Yên Lãng | Thành phố Hà Nội', 2000);
                    """;

            jdbcTemplate.execute(restoreSql);

            return ResponseEntity.ok(Map.of("success", true, "message", "Đã khôi phục bảng users thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
