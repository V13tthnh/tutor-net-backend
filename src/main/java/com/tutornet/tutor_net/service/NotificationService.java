package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.response.NotificationResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    UserRoleResponse.PageResponse<NotificationResponse> getAll(
            Long userId, Boolean isRead, Pageable pageable);
    void send(User user, String type, String title, String body, String data);
    List<NotificationResponse> getUnread(Long userId);
    long countUnread(Long userId);
    void markAllRead(Long userId);
    void markOneRead(Long notificationId, Long userId);
}
