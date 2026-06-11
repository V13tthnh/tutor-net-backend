package com.tutornet.tutor_net.mapper;

import com.tutornet.tutor_net.dto.response.ClassApplicationResponse;
import com.tutornet.tutor_net.entity.ClassApplication;
import org.springframework.stereotype.Component;

@Component
public class ClassApplicationMapper {

    public ClassApplicationResponse toResponse(ClassApplication entity) {
        if (entity == null) return null;

        return new ClassApplicationResponse(
                entity.getId(),
                entity.getClassRequest().getId(),
                entity.getTutor().getId(),
                entity.getTutor().getUser().getFullName(),
                entity.getTutor().getUser().getAvatarUrl(),
                entity.getTutor().getUniversity(),
                entity.getTutor().getMajor(),
                entity.getStatus(),
                entity.getMessage(),
                entity.getCreatedAt()
        );
    }
}