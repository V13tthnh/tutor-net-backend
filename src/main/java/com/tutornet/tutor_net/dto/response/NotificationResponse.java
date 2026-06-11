package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.entity.Notification;
import lombok.Builder;

import java.time.Instant;

@Builder
public record NotificationResponse(
            Long id,
            String type,
            String title,
            String body,
            String data,        // raw JSON string, frontend tự parse
            boolean isRead,     // derive từ readAt
            Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                    .id(n.getId())
                    .type(n.getType())
                    .title(n.getTitle())
                    .body(n.getBody())
                    .data(n.getData())
                    .isRead(n.getReadAt() != null)  // ← derive ở đây
                    .createdAt(n.getCreatedAt())
                    .build();
        }
}

