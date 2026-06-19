package com.tutornet.tutor_net.mapper;

import com.tutornet.tutor_net.dto.response.AdminContractResponse;
import com.tutornet.tutor_net.dto.response.ContractResponse;
import com.tutornet.tutor_net.entity.Contract;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    /**
     * Dùng cho API lấy danh sách hợp đồng có currentUserId để xác định đối tác
     */
    public ContractResponse toResponse(Contract entity, Long currentUserId) {
        if (entity == null) return null;

        String partnerName = "";

        if (entity.getTutor() != null && entity.getTutor().getUser().getId().equals(currentUserId)) {
            partnerName = entity.getClassRequest().getContactName();
        } else if (entity.getTutor() != null) {
            partnerName = entity.getTutor().getUser().getFullName();
        }

        return ContractResponse.builder()
                .id(entity.getId())
                .contractNumber(entity.getContractNumber())

                // Fields mới cho giao diện bảng
                .classCode(entity.getClassRequest().getClassCode())
                .subjectName(entity.getClassRequest().getSubject().getName())
                .partnerName(partnerName)

                // Fields cũ của bạn giữ nguyên
                .targetTutorId(entity.getTutor() != null ? entity.getTutor().getId() : null)
                .contactName(entity.getClassRequest().getContactName())
                .contactPhone(entity.getClassRequest().getContactPhone())

                .introductionFee(entity.getIntroductionFee())
                .effectiveDate(entity.getEffectiveDate())
                .feePaymentDeadline(entity.getFeePaymentDeadline())
                .endDate(entity.getEndDate())
                .freeTrialCount(entity.getFreeTrialCount())

                .status(entity.getStatus())
                .contractFileUrl(entity.getContractFileUrl())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Hàm Overload dùng cho luồng createDraftContract (Không cần currentUserId)
     */
    public ContractResponse toResponse(Contract entity) {
        // Mặc định đối tác truyền vào null, frontend tự dùng contactName hoặc targetTutorName
        return toResponse(entity, -1L);
    }

    /**
     * Dùng cho giao diện quản trị (Admin Dashboard)
     */
    public AdminContractResponse toAdminResponse(Contract entity) {
        if (entity == null) return null;

        return AdminContractResponse.builder()
                .id(entity.getId())
                .contractNumber(entity.getContractNumber())
                .classCode(entity.getClassRequest() != null ? entity.getClassRequest().getClassCode() : null)
                .subjectName((entity.getClassRequest() != null && entity.getClassRequest().getSubject() != null) ? entity.getClassRequest().getSubject().getName() : null)

                .tutorId(entity.getTutor() != null ? entity.getTutor().getId() : null)
                .tutorName((entity.getTutor() != null && entity.getTutor().getUser() != null) ? entity.getTutor().getUser().getFullName() : null)
                .tutorPhone((entity.getTutor() != null && entity.getTutor().getUser() != null) ? entity.getTutor().getUser().getPhone() : null)
                .tutorEmail((entity.getTutor() != null && entity.getTutor().getUser() != null) ? entity.getTutor().getUser().getEmail() : null)

                .contactName(entity.getClassRequest() != null ? entity.getClassRequest().getContactName() : null)
                .contactPhone(entity.getClassRequest() != null ? entity.getClassRequest().getContactPhone() : null)

                .introductionFee(entity.getIntroductionFee())
                .endDate(entity.getEndDate())
                .isFeePaid(entity.getIsFeePaid())
                .paidAt(entity.getPaidAt())
                .feePaymentDeadline(entity.getFeePaymentDeadline())
                .status(entity.getStatus())
                .signedAt(entity.getSignedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}