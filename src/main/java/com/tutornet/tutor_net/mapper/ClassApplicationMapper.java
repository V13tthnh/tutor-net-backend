package com.tutornet.tutor_net.mapper;

import com.tutornet.tutor_net.dto.response.ClassApplicationResponse;
import com.tutornet.tutor_net.entity.ClassApplication;
import org.springframework.stereotype.Component;

@Component
public class ClassApplicationMapper {

    public ClassApplicationResponse toResponse(ClassApplication entity) {
        if (entity == null) return null;

        return ClassApplicationResponse.builder()
                .id(entity.getId())
                .classRequestId(entity.getClassRequest().getId())
                .tutorId(entity.getTutor().getId())
                .tutorName(entity.getTutor().getUser().getFullName())
                .tutorAvatarUrl(entity.getTutor().getUser().getAvatarUrl())
                .university(entity.getTutor().getUniversity())
                .major(entity.getTutor().getMajor())
                .headline(entity.getTutor().getHeadline())
                .experienceYears(entity.getTutor().getExperienceYears())
                .status(entity.getStatus())
                .message(entity.getMessage())
                .appliedAt(entity.getCreatedAt())
                .build();
    }
}