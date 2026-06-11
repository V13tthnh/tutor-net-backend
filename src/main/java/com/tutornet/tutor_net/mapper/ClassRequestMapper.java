package com.tutornet.tutor_net.mapper;

import com.tutornet.tutor_net.dto.request.ClassRequest.CreateClassRequest;
import com.tutornet.tutor_net.dto.response.ClassRequestResponse;
import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.enums.TeachingMode;
import com.tutornet.tutor_net.util.AddressUtils;
import org.springframework.stereotype.Component;

@Component
public class ClassRequestMapper {

    public ClassRequest toEntity(CreateClassRequest request) {
        if (request == null) return null;

        return ClassRequest.builder()
                .contactName(request.contactName())
                .contactPhone(request.contactPhone())
                .contactEmail(request.contactEmail())
                .gradeLevel(request.gradeLevel())
                .proposedPrice(request.proposedPrice())
                .teachingMode(TeachingMode.valueOf(request.teachingMode().toUpperCase()))
                .addressDetail(request.addressDetail())
                .studentNotes(request.studentNotes())
                .build();
    }

    public ClassRequestResponse toResponse(ClassRequest entity) {
        if (entity == null) return null;

        Long userId = (entity.getUser() != null) ? entity.getUser().getId() : null;
        Long tutorId = (entity.getTargetTutor() != null) ? entity.getTargetTutor().getId() : null;
        String tutorName = (entity.getTargetTutor() != null) ? entity.getTargetTutor().getUser().getFullName() : null;
        AddressUtils.Parts currentAddr = AddressUtils.parse(entity.getAddressDetail());

        return new ClassRequestResponse(
                entity.getId(),
                entity.getClassCode(),
                userId,
                entity.getContactName(),
                entity.getContactPhone(),
                entity.getContactEmail(),
                entity.getSubject().getId(),
                entity.getSubject().getName(),
                entity.getGradeLevel(),
                entity.getProposedPrice(),

                entity.getHourlyRate(),
                entity.getSessionsPerWeek(),
                entity.getDurationMinutes(),
                entity.getTeachingMode(),

                currentAddr.province(),
                currentAddr.ward(),
                currentAddr.address(),

                entity.getStudentNotes(),
                tutorId,
                tutorName,
                entity.getStatus(),
                entity.getRejectionReason(),
                0, // Lớp mới đăng thì số lượng gia sư ứng tuyển mặc định bằng 0
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Dùng riêng cho việc render danh sách Job Board, nạp động số lượng người ứng tuyển
     */
    public ClassRequestResponse toResponseWithCount(ClassRequest entity, int totalApplicants) {
        if (entity == null) return null;

        Long userId = (entity.getUser() != null) ? entity.getUser().getId() : null;
        Long tutorId = (entity.getTargetTutor() != null) ? entity.getTargetTutor().getId() : null;
        String tutorName = (entity.getTargetTutor() != null) ? entity.getTargetTutor().getUser().getFullName() : null;
        AddressUtils.Parts currentAddr = AddressUtils.parse(entity.getAddressDetail());


        return new ClassRequestResponse(
                entity.getId(),
                entity.getClassCode(),
                userId,
                entity.getContactName(),
                "***" + entity.getContactPhone().substring(entity.getContactPhone().length() - 3), // Che SĐT trên bảng tin công khai
                entity.getContactEmail() != null ? "***@***.com" : null, // Che Email
                entity.getSubject().getId(),
                entity.getSubject().getName(),
                entity.getGradeLevel(),
                entity.getProposedPrice(),
                entity.getHourlyRate(),
                entity.getSessionsPerWeek(),
                entity.getDurationMinutes(),

                entity.getTeachingMode(),
                currentAddr.province(),
                currentAddr.ward(),
                currentAddr.address(),
                entity.getStudentNotes(),
                tutorId,
                tutorName,
                entity.getStatus(),
                entity.getRejectionReason(),
                totalApplicants, // Nạp số lượng đếm được từ DB
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
