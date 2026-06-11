package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.service.impl.FileStorageServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageServiceImpl fileStorageService;

    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String url = fileStorageService.storeAvatar(file);
        return ResponseEntity.ok(ApiResponse.ok(new UploadResponse(url)));
    }

    @PostMapping("/document")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String url = fileStorageService.storeDocument(file);
        return ResponseEntity.ok(ApiResponse.ok(new UploadResponse(url)));
    }

    public record UploadResponse(String url) {}
}
