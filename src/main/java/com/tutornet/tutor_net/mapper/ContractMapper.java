package com.tutornet.tutor_net.mapper;

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
}