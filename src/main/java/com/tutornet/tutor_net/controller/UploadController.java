package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.config.FileStorageProperties;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.service.impl.FileStorageServiceImpl;
import com.tutornet.tutor_net.util.SecuritySandboxHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageServiceImpl fileStorageService;
    private final FileStorageProperties props;

    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        List<String> activeFlags = SecuritySandboxHelper.getActiveFlags();
        
        // Kịch bản lỗi: upload_webshell hoặc ext_bypass hoặc mime_spoofing được bật
        if (activeFlags.contains("upload_webshell") || activeFlags.contains("ext_bypass") || activeFlags.contains("mime_spoofing")) {
            log.warn("[DEMO-SANDBOX] Bật chế độ VULNERABLE cho Avatar upload. Bỏ qua các bước validate an toàn.");
            
            if (file.isEmpty()) {
                throw new BusinessException("File không được rỗng");
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null || filename.trim().isEmpty()) {
                filename = "avatar_" + System.currentTimeMillis();
            }
            
            Path uploadPath = Paths.get(props.getDir(), "avatars");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            String url = "/uploads/avatars/" + filename;
            return ResponseEntity.ok(ApiResponse.ok(new UploadResponse(url)));
        }

        // Chế độ SAFE
        String url = fileStorageService.storeAvatar(file);
        return ResponseEntity.ok(ApiResponse.ok(new UploadResponse(url)));
    }

    @PostMapping("/document")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        List<String> activeFlags = SecuritySandboxHelper.getActiveFlags();

        // upload_webshell hoặc ext_bypass hoặc mime_spoofing được bật
        if (activeFlags.contains("upload_webshell") || activeFlags.contains("ext_bypass") || activeFlags.contains("mime_spoofing")) {
            log.warn("[DEMO-SANDBOX] Bật chế độ VULNERABLE cho Document upload. Bỏ qua các bước validate an toàn.");

            if (file.isEmpty()) {
                throw new BusinessException("File không được rỗng");
            }

            String filename = file.getOriginalFilename();
            if (filename == null || filename.trim().isEmpty()) {
                filename = "doc_" + System.currentTimeMillis();
            }

            Path uploadPath = Paths.get(props.getDir(), "documents");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String url = "/uploads/documents/" + filename;
            return ResponseEntity.ok(ApiResponse.ok(new UploadResponse(url)));
        }

        // Chế độ SAFE (Mặc định)
        String url = fileStorageService.storeDocument(file);
        return ResponseEntity.ok(ApiResponse.ok(new UploadResponse(url)));
    }

    @GetMapping("/files/download")
    public ResponseEntity<?> downloadFile(@RequestParam("filename") String filename) {
        List<String> activeFlags = SecuritySandboxHelper.getActiveFlags();
        
        // path_traversal được bật -> Nối chuỗi trực tiếp
        if (activeFlags.contains("path_traversal")) {
            log.warn("[DEMO-SANDBOX] Bật chế độ VULNERABLE cho File Download. Thực thi Path Traversal.");
            try {
                Path filePath = Paths.get(props.getDir(), "documents").resolve(filename);
                if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                    byte[] bytes = Files.readAllBytes(filePath);
                    return ResponseEntity.ok()
                            .header("Content-Disposition", "attachment; filename=\"" + filePath.getFileName() + "\"")
                            .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                            .body(bytes);
                } else {
                    return ResponseEntity.status(404).body("File not found at resolved: " + filePath.toString());
                }
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error: " + e.getMessage());
            }
        }
        
        // SAFE: Chặn .. và / và \
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return ResponseEntity.status(400).body("Từ chối: Tên file chứa ký tự không hợp lệ!");
        }
        
        try {
            Path documentsDir = Paths.get(props.getDir(), "documents").normalize().toAbsolutePath();
            Path filePath = documentsDir.resolve(filename).normalize().toAbsolutePath();
            
            if (!filePath.startsWith(documentsDir)) {
                return ResponseEntity.status(403).body("Từ chối: Quyền truy cập bị giới hạn!");
            }
            
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                byte[] bytes = Files.readAllBytes(filePath);
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + filePath.getFileName() + "\"")
                        .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                        .body(bytes);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
        
        return ResponseEntity.status(404).body("File not found");
    }

    @PostMapping("/ping-website")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> pingWebsite(@RequestParam("host") String host) {
        List<String> activeFlags = SecuritySandboxHelper.getActiveFlags();
        
        // lỗi: os_command được bật -> Nối chuỗi chạy shell
        if (activeFlags.contains("os_command")) {
            log.warn("[DEMO-SANDBOX] Bật chế độ VULNERABLE cho Ping Website. Thực thi Command Injection.");
            String command = "ping -c 1 " + host;
            boolean hasInjection = host.contains(";") || host.contains("&") || host.contains("|") || host.contains("`") || host.contains("$");
            
            // Simulate hoặc execute
            String output;
            if (hasInjection) {
                output = simulateCommandExecution(command);
            } else {
                output = executeRealPing(host);
            }
            return ResponseEntity.ok(Map.of(
                    "command", command,
                    "output", output,
                    "injection", true
            ));
        }
        
        // SAFE: Regex check + ProcessBuilder
        java.util.regex.Pattern validHost = java.util.regex.Pattern.compile("^[a-zA-Z0-9.-]+$");
        if (!validHost.matcher(host).matches()) {
            return ResponseEntity.status(400).body(Map.of("error", "Địa chỉ host không hợp lệ (Chỉ cho phép IP/Domain sạch)!"));
        }
        
        String output = executeRealPing(host);
        return ResponseEntity.ok(Map.of(
                "command", "ProcessBuilder ping " + host,
                "output", output,
                "injection", false
        ));
    }
    
    private String executeRealPing(String host) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("ping", "-n", "1", "-w", "1000", host);
            } else {
                pb = new ProcessBuilder("ping", "-c", "1", "-W", "1", host);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            return new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Ping failed: " + e.getMessage();
        }
    }
    
    private String simulateCommandExecution(String command) {
        if (command.contains("passwd")) {
            return "PING output...\nroot:x:0:0:root:/root:/bin/bash\nwww-data:x:33:33::/var/www  [SIMULATED - COMMAND INJECTION SUCCESS]";
        } else if (command.contains("env") || command.contains(".env")) {
            return "PING output...\nDB_PASSWORD=Sup3rS3cr3t\nJWT_SECRET=my-secret-key-123\n[SIMULATED - ENVIRONMENT EXFILTRATED]";
        } else if (command.contains("id") || command.contains("whoami")) {
            return "PING output...\ntutornet-app-user  [SIMULATED - COMMAND INJECTION SUCCESS]";
        } else if (command.contains("ls")) {
            return "PING output...\nsrc\ntarget\nuploads\npom.xml  [SIMULATED - COMMAND INJECTION SUCCESS]";
        }
        return "PING output...\n[Command executed successfully - SIMULATED]";
    }

    public record UploadResponse(String url) {}
}
