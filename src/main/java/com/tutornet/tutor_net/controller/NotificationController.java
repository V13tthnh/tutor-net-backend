package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.NotificationResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.NotificationService;
import com.tutornet.tutor_net.util.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserRoleResponse.PageResponse<NotificationResponse>>> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Pageable pageable = PageableUtils.build(page, size, null, "createdAt", "desc");
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getAll(currentUser.getUser().getId(), isRead, pageable)));
    }

    @PatchMapping("/{id}/mark-read")
    public ResponseEntity<Void> markOneRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        notificationService.markOneRead(id, currentUser.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnread(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getUnread(currentUser.getUser().getId())));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> countUnread(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.countUnread(currentUser.getUser().getId())));
    }

    @PatchMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        notificationService.markAllRead(currentUser.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}