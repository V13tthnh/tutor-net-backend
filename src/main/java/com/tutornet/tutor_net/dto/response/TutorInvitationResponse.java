    package com.tutornet.tutor_net.dto.response;

    import com.tutornet.tutor_net.enums.InvitationStatus;
    import lombok.Builder;
    import lombok.Getter;

    import java.math.BigDecimal;
    import java.time.Instant;

    @Getter
    @Builder
    public class TutorInvitationResponse {

        private Long id;

        // Thông tin học viên (Trích xuất từ ClassRequest)
        private Long studentUserId;      // null nếu là khách vãng lai
        private String studentName;
        private String studentPhone;
        private String studentEmail;

        // Thông tin cơ bản của lớp học (Trích xuất từ ClassRequest)
        private String subjectName;
        private BigDecimal proposedPrice;

        // Nội dung lời mời
        private String message;

        // Trạng thái của lời mời
        private InvitationStatus status;

        private Instant createdAt;

        @Builder
        public record AdminTutorInvitationTableResponse(
                Long id,
                String classCode,
                String subjectName,
                BigDecimal proposedPrice,

                // Thông tin Phụ huynh (Người gửi)
                String studentName,
                String studentPhone,

                // Thông tin Gia sư (Người nhận)
                Long tutorId,
                String tutorName,

                String message,
                InvitationStatus status,
                Instant createdAt,
                String cancelReason // Hiển thị lý do nếu bị Admin hủy
        ) {}
    }