package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.response.NotificationResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.entity.Notification;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.repository.NotificationRepository;
import com.tutornet.tutor_net.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public UserRoleResponse.PageResponse<NotificationResponse> getAll(
            Long userId, Boolean isRead, Pageable pageable) {

        Page<Notification> page;

        if (isRead == null) {
            // Không filter — trả tất cả
            page = notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else if (Boolean.FALSE.equals(isRead)) {
            page = notificationRepository.findUnreadByUserId(userId, pageable);
        } else {
            page = notificationRepository.findReadByUserId(userId, pageable);
        }

        List<NotificationResponse> content = page.getContent()
                .stream()
                .map(NotificationResponse::from)
                .toList();

        return new UserRoleResponse.PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

    @Override
    @Async("notificationExecutor")
    @Transactional
    public void send(User user, String type, String title, String body, String data) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .data(data) // ví dụ: """{"session_id": 42}"""
                .build();
        notificationRepository.save(notification);

        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/notifications",
                NotificationResponse.from(notification));
    }

    @Override
    public List<NotificationResponse> getUnread(Long userId) {
        return notificationRepository
                .findTop20ByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId, Instant.now());
    }

    @Override
    @Transactional
    public void markOneRead(Long notificationId, Long userId) {
        // Kiểm tra tồn tại và ownership trước khi update
        notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(userId))
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));

        notificationRepository.markOneReadByIdAndUserId(notificationId, userId, Instant.now());
    }
}
