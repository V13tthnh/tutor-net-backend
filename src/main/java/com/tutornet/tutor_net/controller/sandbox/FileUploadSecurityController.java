package com.tutornet.tutor_net.controller.sandbox;

import com.tutornet.tutor_net.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Chương 9 — FILE UPLOAD SECURITY
 *
 * Endpoints:
 *   POST /api/v1/demo/upload/vulnerable    — Không validate gì cả
 *   POST /api/v1/demo/upload/safe          — Validate đuôi file, magic bytes và ngăn chặn thực thi
 *   GET  /api/v1/demo/upload/files         — Liệt kê danh sách file đã upload
 *   DELETE /api/v1/demo/upload/files/{filename} — Xoá file demo
 *   GET  /api/v1/demo/upload/execute       — Giả lập thực thi command qua webshell
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/demo/upload")
@RequiredArgsConstructor
public class FileUploadSecurityController {

    private final FileStorageProperties props;

    // Allowed extensions for SAFE mode
    private static final List<String> ALLOWED_EXTS = Arrays.asList("jpg", "jpeg", "png", "gif", "pdf");

    // Allowed Content-Types for SAFE mode
    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "application/pdf");

    // ----------------------------------------------------------------
    // 1. Vulnerable Upload (Chấp nhận mọi file, kể cả webshell .php)
    // ----------------------------------------------------------------
    @PostMapping("/vulnerable")
    public ResponseEntity<?> vulnerableUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File không được trống"));
        }

        try {
            Path sandboxPath = Paths.get(props.getDir(), "sandbox");
            if (!Files.exists(sandboxPath)) {
                Files.createDirectories(sandboxPath);
            }

            String filename = file.getOriginalFilename();
            if (filename == null || filename.trim().isEmpty()) {
                filename = "uploaded_file_" + System.currentTimeMillis();
            }

            Path filePath = sandboxPath.resolve(filename);
            // Ghi đè file nếu đã tồn tại để tránh quá tải
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.warn("[DEMO-CH9] VULNERABLE UPLOAD: File '{}' được lưu thành công không qua kiểm tra.", filename);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Upload thành công (Vulnerable Mode)",
                    "filename", filename,
                    "url", "/uploads/sandbox/" + filename
            ));
        } catch (IOException e) {
            log.error("Failed to save vulnerable upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi lưu file: " + e.getMessage()));
        }
    }

    // ----------------------------------------------------------------
    // 2. Safe Upload (Validate đuôi file cuối cùng & Magic Bytes)
    // ----------------------------------------------------------------
    @PostMapping("/safe")
    public ResponseEntity<?> safeUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File không được trống"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tên file không hợp lệ"));
        }

        // A. Kiểm tra Extension cuối cùng (Chặn đứng Extension Bypass như shell.php.jpg)
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File bắt buộc phải có đuôi mở rộng"));
        }
        String ext = filename.substring(lastDot + 1).toLowerCase();
        if (!ALLOWED_EXTS.contains(ext)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Từ chối: Đuôi file ." + ext + " không được hỗ trợ"));
        }

        // B. Kiểm tra Magic Bytes đầu file (Chặn đứng MIME Spoofing)
        if (!validateMagicBytes(file)) {
            log.error("[DEMO-CH9] MIME Spoofing detected for file '{}'!", filename);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Từ chối: Cấu trúc tệp tin thực tế không khớp với định dạng ảnh/tài liệu (MIME Spoofing)"));
        }

        try {
            Path sandboxPath = Paths.get(props.getDir(), "sandbox");
            if (!Files.exists(sandboxPath)) {
                Files.createDirectories(sandboxPath);
            }

            // Thay đổi tên file sang UUID ngẫu nhiên để chống gián lộ đường dẫn (Path Disclosure / Execution)
            String safeFilename = UUID.randomUUID().toString() + "." + ext;
            Path filePath = sandboxPath.resolve(safeFilename);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[DEMO-CH9] SAFE UPLOAD: File '{}' được lưu thành công dưới tên '{}'", filename, safeFilename);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Upload thành công (Safe Mode)",
                    "filename", safeFilename,
                    "url", "/uploads/sandbox/" + safeFilename
            ));
        } catch (IOException e) {
            log.error("Failed to save safe upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi lưu file: " + e.getMessage()));
        }
    }

    // Helper kiểm tra Magic Bytes và cấu trúc tệp tin cơ bản
    private boolean validateMagicBytes(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 4) return false;

            // Kiểm tra chữ ký file hoặc tag script cơ bản để ngăn mã độc PHP/HTML
            String contentSample = new String(bytes, 0, Math.min(bytes.length, 100));
            if (contentSample.contains("<?php") || contentSample.contains("<?") || contentSample.contains("<script")) {
                return false; // Phát hiện thẻ script executable
            }

            String contentType = file.getContentType();
            if (contentType == null) return false;

            if (contentType.equals("image/jpeg")) {
                return bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8;
            } else if (contentType.equals("image/png")) {
                return bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47;
            } else if (contentType.equals("application/pdf")) {
                return bytes[0] == (byte) 0x25 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x44 && bytes[3] == (byte) 0x46;
            } else if (contentType.equals("image/gif")) {
                return bytes[0] == (byte) 'G' && bytes[1] == (byte) 'I' && bytes[2] == (byte) 'F';
            }

            return ALLOWED_TYPES.contains(contentType);
        } catch (IOException e) {
            return false;
        }
    }

    // ----------------------------------------------------------------
    // 3. Terminal webshell execution simulator (Demo hậu quả thực tế)
    // ----------------------------------------------------------------
    @GetMapping("/execute")
    public ResponseEntity<?> executeCommand(
            @RequestParam String filename,
            @RequestParam(defaultValue = "id") String cmd) {

        Path sandboxPath = Paths.get(props.getDir(), "sandbox");
        Path filePath = sandboxPath.resolve(filename);

        if (!Files.exists(filePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "File webshell không tồn tại hoặc đã bị xoá"));
        }

        // Chỉ cho phép "thực thi" nếu file có đuôi mở rộng độc hại (php, php5, phtml) đã upload thành công
        String lowerName = filename.toLowerCase();
        if (!lowerName.endsWith(".php") && !lowerName.endsWith(".php5") && !lowerName.endsWith(".phtml")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Chỉ các file script (.php, .php5) mới có khả năng biên dịch và thực thi câu lệnh trên Web Server."
            ));
        }

        log.warn("[DEMO-CH9] WEBSHELL EXECUTION: Chạy lệnh '{}' thông qua file '{}'", cmd, filename);

        // Giả lập kết quả các command tương ứng
        String cleanCmd = cmd.trim();
        String output;
        switch (cleanCmd) {
            case "id":
                output = "uid=33(www-data) gid=33(www-data) groups=33(www-data)";
                break;
            case "whoami":
                output = "www-data";
                break;
            case "ls":
            case "ls -la":
                output = "total 28\n" +
                        "drwxr-xr-x  3 www-data www-data 4096 Jun 28 15:44 .\n" +
                        "drwxr-xr-x 12 www-data www-data 4096 Jun 28 15:44 ..\n" +
                        "-rw-r--r--  1 www-data www-data   76 Jun 28 15:44 .env\n" +
                        "-rw-r--r--  1 www-data www-data  142 Jun 28 15:44 index.php\n" +
                        "drwxrwxrwx  2 www-data www-data 4096 Jun 28 15:44 sandbox\n" +
                        "-rw-r--r--  1 www-data www-data  150 Jun 28 15:44 " + filename;
                break;
            case "cat .env":
                output = "DATABASE_URL=postgresql://postgres_admin:supersecretpwd@localhost:5432/tutornet_db\n" +
                        "JWT_SECRET=TutorNetDevelopmentVeryLongSecretKeyForJWTSigning123456\n" +
                        "SPRING_MAIL_PASSWORD=tutornet_smtp_token_129847129";
                break;
            case "cat /etc/passwd":
                output = "root:x:0:0:root:/root:/bin/bash\n" +
                        "daemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin\n" +
                        "bin:x:2:2:bin:/bin:/usr/sbin/nologin\n" +
                        "sys:x:3:3:sys:/dev:/usr/sbin/nologin\n" +
                        "sync:x:4:65534:sync:/bin:/bin/sync\n" +
                        "www-data:x:33:33:www-data:/var/www:/usr/sbin/nologin\n" +
                        "johnsnow:x:1000:1000:John Snow,,,:/home/johnsnow:/bin/bash";
                break;
            default:
                output = "sh: " + cleanCmd + ": command not found";
                break;
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "command", cleanCmd,
                "output", output
        ));
    }

    // ----------------------------------------------------------------
    // 4. File utilities
    // ----------------------------------------------------------------
    @GetMapping("/files")
    public ResponseEntity<?> listFiles() {
        Path sandboxPath = Paths.get(props.getDir(), "sandbox");
        if (!Files.exists(sandboxPath)) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        try (var stream = Files.list(sandboxPath)) {
            List<Map<String, Object>> files = stream
                    .filter(Files::isRegularFile)
                    .map(p -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", p.getFileName().toString());
                        try {
                            map.put("size", Files.size(p));
                            map.put("uploadedAt", new Date(Files.getLastModifiedTime(p).toMillis()).toString());
                        } catch (IOException ignored) {}
                        return map;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Không thể đọc danh sách file"));
        }
    }

    @DeleteMapping("/files/{filename}")
    public ResponseEntity<?> deleteFile(@PathVariable String filename) {
        Path sandboxPath = Paths.get(props.getDir(), "sandbox");
        Path filePath = sandboxPath.resolve(filename);

        try {
            if (Files.deleteIfExists(filePath)) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Đã xoá file thành công"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "File không tồn tại"));
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi khi xoá file: " + e.getMessage()));
        }
    }
}
