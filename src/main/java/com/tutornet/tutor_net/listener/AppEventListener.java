package com.tutornet.tutor_net.listener;

import com.tutornet.tutor_net.mail.payload.*;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.event.*;
import com.tutornet.tutor_net.mail.senders.*;
import com.tutornet.tutor_net.repository.ContractRepository;
import com.tutornet.tutor_net.repository.UserRepository;
import com.tutornet.tutor_net.service.ContractService;
import com.tutornet.tutor_net.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ContractService contractService;
    private final ContractRepository contractRepository;

    private final VerificationEmailSender verificationEmailSender;
    private final TutorApprovedEmailSender tutorApprovedEmailSender;
    private final TutorRejectedEmailSender tutorRejectedEmailSender;
    private final ReviewRequestEmailSender reviewRequestEmailSender;
    private final ContractAttachmentEmailSender contractAttachmentEmailSender;
    private final TutorAppliedEmailSender tutorAppliedEmailSender;
    private final PasswordResetEmailSender passwordResetEmailSender;
    private final TutorInvitedEmailSender tutorInvitedEmailSender;
    private final TutorAcceptedInvitationEmailSender tutorAcceptedInvitationEmailSender;
    private final TutorApplicationAcceptedEmailSender tutorApplicationAcceptedEmailSender;
    private final ApplicationRejectedByAdminEmailSender applicationRejectedByAdminEmailSender;

    // ── Auth events ──

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Gửi mail xác thực tới {}", event.email());
        verificationEmailSender.execute(event.email(), new VerificationPayload(event.verificationToken()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Gửi mail reset password tới {}", event.email());
        PasswordResetPayload payload = new PasswordResetPayload(event.fullName(), event.resetToken());
        passwordResetEmailSender.execute(event.email(), payload);
    }

    // ── Tutor review events ──

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTutorReviewed(TutorReviewedEvent event) {
        TutorReviewMailPayload payload = new TutorReviewMailPayload(event.tutorFullName(), event.rejectionReason());
        switch (event.newStatus()) {
            case APPROVED -> {
                tutorApprovedEmailSender.execute(event.tutorEmail(), payload);
                notificationService.send(
                        event.tutorUser(),
                        "tutor_approved",
                        "Hồ sơ đã được duyệt",
                        "Chúc mừng! Hồ sơ gia sư của bạn đã được chấp thuận.",
                        """
                                {"redirect": "/tutor/dashboard"}
                             """
                );
            }
            case REJECTED -> {
                tutorRejectedEmailSender.execute(event.tutorEmail(), payload);
                notificationService.send(
                        event.tutorUser(),
                        "tutor_rejected",
                        "Hồ sơ chưa được duyệt",
                        "Lý do: " + event.rejectionReason(),
                        """
                                {"redirect": "/tutor/profile/edit"}
                             """
                );
            }
            default -> log.warn("Không có handler cho status={}", event.newStatus());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTutorSubmittedForReview(TutorSubmittedForReviewEvent event) {
        // Lấy tất cả user có role admin/super_admin
        List<User> admins = userRepository.findAllAdmins();

        admins.forEach(admin ->
                notificationService.send(
                        admin,
                        "tutor_submitted",
                        "Hồ sơ gia sư mới",
                        event.tutorFullName() + " vừa nộp hồ sơ chờ duyệt.",
                        "{\"redirect\": \"/admin/tutors/" + event.tutorProfileId() + "\"}"
                )
        );
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClassRequestSaved(ClassRequestNotificationEvent event) {

        if (event.targetTutorUser() != null) {
            notificationService.send(
                    event.targetTutorUser(),
                    "class_request_direct",
                    "Bạn có lời mời dạy mới \uD83C\uDF89",
                    "Có học viên vừa gửi lời mời bạn dạy môn " + event.subjectName(),
                    "{\"redirect\": \"/tutor/class-requests/" + event.classRequestId() + "\"}"
            );

        } else {
            // Gửi Notif cho toàn bộ Admin
            List<User> admins = userRepository.findAllAdmins();
            admins.forEach(admin ->
                    notificationService.send(
                            admin,
                            "class_request_public",
                            "Yêu cầu tìm gia sư mới",
                            "Có một yêu cầu tìm gia sư môn " + event.subjectName() + " mới được đăng lên hệ thống.",
                            "{\"redirect\": \"/admin/class-requests/" + event.classRequestId() + "\"}"
                    )
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTutorRespondedToInvite(TutorRespondedToInviteEvent event) {

        if (event.isAccepted()) {
            log.info("Gia sư {} đã đồng ý nhận lớp {}", event.tutorName(), event.classRequestId());

            // Gửi thông báo email và in-app cho học viên
            if (event.studentUser() != null) {
                notificationService.send(
                        event.studentUser(),
                        "invite_accepted",
                        "Gia sư đã đồng ý nhận lớp!",
                        "Gia sư " + event.tutorName() + " đã đồng ý yêu cầu dạy học của bạn.",
                        "{\"redirect\": \"/student/classes/" + event.classRequestId() + "\"}"
                );
            }

            // TỰ ĐỘNG KÍCH HOẠT SINH HỢP ĐỒNG NHÁP
            try {
                log.info("Bắt đầu tự động tạo hợp đồng nháp (DRAFT) cho mã lớp: {}", event.classRequestId());
                contractService.createDraftContract(event.classRequestId());
                log.info("Tạo hợp đồng nháp thành công hệ thống.");
            } catch (Exception e) {
                // Sử dụng khối try-catch để log lỗi riêng biệt, đảm bảo nếu luồng sinh hợp đồng
                // gặp sự cố thì không làm ảnh hưởng hay crash luồng bắn thông báo chính của hệ thống
                log.error("Lỗi nghiêm trọng khi tự động sinh hợp đồng nháp: {}", e.getMessage());
            }

        } else {
            // Trường hợp Gia sư từ chối nhận lớp (Logic cũ giữ nguyên)
            log.info("Gia sư {} đã từ chối nhận lớp {}", event.tutorName(), event.classRequestId());
            if (event.studentUser() != null) {
                notificationService.send(
                        event.studentUser(),
                        "invite_rejected",
                        "Gia sư không thể nhận lớp",
                        "Rất tiếc, gia sư " + event.tutorName() + " hiện không thể nhận yêu cầu của bạn.",
                        "{\"redirect\": \"/tutors/search\"}"
                );
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClassRequestReviewed(ClassRequestReviewedEvent event) {
        // Xác định tiêu đề và nội dung thông báo dựa trên kết quả duyệt
        String title = event.isApproved() ? "Lớp học của bạn đã được duyệt!" : "Yêu cầu lớp học bị từ chối";

        String body = event.isApproved()
                ? "Yêu cầu tìm gia sư môn " + event.subjectName() + " đã được phê duyệt và hiển thị công khai trên hệ thống."
                : "Yêu cầu tìm gia sư môn " + event.subjectName() + " không được duyệt. Lý do: " + event.rejectionReason();

        // 2. Tạo chuỗi JSON data chứa redirect URL khớp với cấu trúc xử lý của useNotifications.ts
        String dataJson = "{\"redirect\": \"/student/requests/" + event.classRequestId() + "\"}";

        // 3. Gọi dịch vụ thông báo đẩy qua WebSocket tới hàng đợi /user/queue/notifications
        notificationService.send(
                event.recipient(),
                "class_review_result",
                title,
                body,
                dataJson
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTutorApplied(TutorAppliedEvent event) {
        if (event.studentUser() != null) {
            // Có tài khoản → gửi Notification in-app
            notificationService.send(event.studentUser(), "new_application",
                    "Có gia sư ứng tuyển!",
                    "Gia sư " + event.tutorName() + " vừa ứng tuyển vào lớp của bạn.",
                    "{\"redirect\": \"/student/requests/" + event.classRequestId() + "\"}");
        } else {
            // Vãng lai → fallback gửi email
            if (event.studentEmail() != null && !event.studentEmail().isBlank()) {
                StudentTutorPayload payload = new StudentTutorPayload(event.studentName(), event.tutorName(), null);
                tutorAppliedEmailSender.execute(event.studentEmail(), payload);
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTutorInvitedEvent(TutorInvitedEvent event) {

        if (event.tutorUser() != null) {
            notificationService.send(
                    event.tutorUser(),
                    "tutor_invited",
                    "Bạn có một lời mời dạy học mới! 🎉",
                    "Học viên " + event.studentName() + " vừa gửi lời mời đến bạn.",
                    "{\"redirect\": \"/tutor/invitations/" + event.invitationId() + "\"}"
            );
        }

        // Gửi email cho gia sư
        TutorInvitedPayload payload = new TutorInvitedPayload(event.tutorName(), event.studentName(), event.message());
        tutorInvitedEmailSender.execute(event.tutorEmail(), payload);
    }


    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContractCompleted(ContractCompletedEvent event) {

        log.info("Hợp đồng {} đã chuyển sang COMPLETED. Bắt đầu gửi yêu cầu đánh giá.", event.contractNumber());

        String magicToken = java.util.UUID.randomUUID().toString() + "-" + event.contractId();
        String reviewLink = "http://localhost:3000/reviews/guest?contractId="
                + event.contractId() + "&token=" + magicToken;

        try {
            contractRepository.updateGuestReviewToken(event.contractId(), magicToken);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật Review Token cho HD {}: {}", event.contractNumber(), e.getMessage());
            return;
        }

        // Gửi notification nếu user có tài khoản
        if (event.studentUserId() != null) {
            userRepository.findById(event.studentUserId()).ifPresent(student -> notificationService.send(
                    student,
                    "contract_completed",
                    "Khóa học hoàn tất! Hãy đánh giá gia sư nhé \uD83C\uDF93",
                    "Bạn cảm thấy Gia sư " + event.tutorName() + " như thế nào? Dành chút thời gian để lại nhận xét nhé.",
                    "{\"redirect\": \"/student/contracts/" + event.contractId() + "?action=review\"}"
            ));
        }

        // Gửi Email
        if (event.studentEmail() != null && !event.studentEmail().isBlank()) {
            ReviewEmailPayload payload = new ReviewEmailPayload(
                    event.studentName(),
                    event.tutorName(),
                    reviewLink
            );
            reviewRequestEmailSender.execute(event.studentEmail(), payload);
            log.info("Đã ủy quyền cho Template Method gửi email Review tới: {}", event.studentEmail());
        } else {
            log.warn("Không tìm thấy Email của học viên trong Hợp đồng {}", event.contractNumber());
        }
    }

    /**
     * Gia sư chấp nhận lời mời → tạo Contract DRAFT + gửi mail/notif cho học viên.
     * Chạy SAU KHI transaction commit (AFTER_COMMIT) để tránh tạo Contract
     * khi DB rollback giữa chừng.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTutorAcceptedInvitation(TutorAcceptedInvitationEvent event) {

        log.info("Gia sư {} đã chấp nhận invitation #{}, classRequest #{}",
                event.tutorName(), event.invitationId(), event.classRequestId());

        // 1. Gửi thông báo in-app cho học viên (nếu có tài khoản)
        if (event.studentUser() != null) {
            notificationService.send(
                    event.studentUser(),
                    "invite_accepted",
                    "Gia sư đã đồng ý nhận lớp!",
                    "Gia sư " + event.tutorName() + " đã chấp nhận lời mời của bạn.",
                    "{\"redirect\": \"/student/classes/" + event.classRequestId() + "\"}"
            );
        }

        // Gửi email thông báo cho học viên (cả khách vãng lai)
        if (event.studentEmail() != null && !event.studentEmail().isBlank()) {
            StudentTutorPayload payload = new StudentTutorPayload(event.studentName(), event.tutorName(), "");
            tutorAcceptedInvitationEmailSender.execute(event.studentEmail(), payload);
        }

        // Tự động tạo Contract DRAFT — dùng try-catch riêng để lỗi Contract
        //    không làm mất thông báo đã gửi cho học viên
        try {
            log.info("Bắt đầu tạo hợp đồng DRAFT cho classRequest #{}", event.classRequestId());
            contractService.createDraftContract(event.classRequestId());
            log.info("Tạo hợp đồng DRAFT thành công cho classRequest #{}", event.classRequestId());
        } catch (Exception e) {
            log.error("Lỗi khi tạo hợp đồng DRAFT cho classRequest #{}: {}",
                    event.classRequestId(), e.getMessage(), e);
        }
    }

    /**
     * Lắng nghe sự kiện hợp đồng được ký kết thành công.
     * Chạy bất đồng bộ (Async) giải phóng luồng chính và chạy sau khi DB đã commit thành công.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContractSignedEvent(ContractSignedEvent event) {
        log.info("Bắt đầu tiến trình gửi email hợp đồng điện tử bất đồng bộ cho mã: {}", event.contractNumber());
        try {
            // Gửi Email đính kèm tệp tin PDF cho Gia sư
            ContractAttachmentPayload tutorPayload = new ContractAttachmentPayload(event.tutorName(), event.contractNumber(), event.pdfBytes());
            contractAttachmentEmailSender.execute(event.tutorEmail(), tutorPayload);

            // Gửi Email đính kèm tệp tin PDF cho học viên (Nếu có email)
            if (event.studentEmail() != null && !event.studentEmail().isBlank()) {
                ContractAttachmentPayload studentPayload = new ContractAttachmentPayload(event.studentName(), event.contractNumber(), event.pdfBytes());
                contractAttachmentEmailSender.execute(event.studentEmail(), studentPayload);
            }
            log.info("Đã gửi email hợp đồng thành công đến các bên liên quan.");
        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi gửi email đính kèm hợp đồng tĩnh: {}", e.getMessage(), e);
        }
    }

    /**
     * Gia sư được chọn (Học viên accept đơn ứng tuyển)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTutorApplicationAccepted(TutorApplicationAcceptedEvent event) {

        log.info("Học viên {} đã chọn gia sư {} cho lớp {}",
                event.studentName(), event.tutorName(), event.classRequestId());

        // 1. Gửi thông báo in-app cho Gia sư
        if (event.tutorUserId() != null) {
            userRepository.findById(event.tutorUserId()).ifPresent(tutor -> notificationService.send(
                    tutor,
                    "application_accepted",
                    "Chúc mừng! Phụ huynh đã chọn bạn 🎉",
                    "Phụ huynh " + event.studentName() + " đã đồng ý chọn bạn dạy lớp của họ. Vui lòng xác nhận hợp đồng để nhận lớp.",
                    "{\"redirect\": \"/account/my-classes\"}" // Dẫn thẳng ra màn quản lý hợp đồng của gia sư
            ));
        }

        // 2. Gửi email thông báo cho Gia sư
        if (event.tutorEmail() != null && !event.tutorEmail().isBlank()) {
            StudentTutorPayload payload = new StudentTutorPayload(event.studentName(), event.tutorName(), "");
            tutorApplicationAcceptedEmailSender.execute(event.tutorEmail(), payload);
        }
    }

    /**
     * Admin ẩn đơn ứng tuyển của gia sư
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTutorApplicationRejectedByAdmin(TutorApplicationRejectedByAdminEvent event) {

        log.info("Admin đã ẩn đơn ứng tuyển #{} của gia sư {}",
                event.getApplicationId(), event.getTutorFullName());

        // 1. Gửi thông báo in-app cho Gia sư
        userRepository.findById(event.getTutorUserId()).ifPresent(tutor -> notificationService.send(
                tutor,
                "application_rejected_by_admin",
                "Đơn ứng tuyển của bạn đã bị từ chối",
                "Đơn ứng tuyển vào lớp của phụ huynh " + event.getClassContactName() + " không được chấp thuận.",
                "{\"redirect\": \"/tutor/applications\"}"
        ));

        // 2. Gửi email thông báo cho Gia sư
        if (event.getTutorEmail() != null && !event.getTutorEmail().isBlank()) {
            ApplicationRejectedPayload payload = new ApplicationRejectedPayload(event.getTutorFullName(), event.getClassContactName());
            applicationRejectedByAdminEmailSender.execute(event.getTutorEmail(), payload);
        }
    }

    // ── Review Submitted Event ──

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNewReviewSubmitted(NewReviewSubmittedEvent event) {

        log.info("Có đánh giá mới {} sao từ {} dành cho Gia sư ID: {}",
                event.rating(), event.reviewerName(), event.tutorUserId());

        // Lấy lại User Gia sư từ DB (tránh Lazy Load)
        User tutor = userRepository.findById(event.tutorUserId()).orElse(null);

        if (tutor != null) {
            String title = event.rating() >= 4 ? "Bạn có một đánh giá tuyệt vời!" : "Bạn có một đánh giá mới";
            String body = event.reviewerName() + " vừa để lại đánh giá " + event.rating() + " sao cho lớp học của bạn.";

            notificationService.send(
                    tutor,
                    "new_review_received",
                    title,
                    body,
                    "{\"redirect\": \"/tutor/reviews\"}" // Dẫn về trang quản lý đánh giá của gia sư
            );
        }

        // Nếu là đánh giá tiêu cực (1-2 sao), báo ngay cho Admin để giải quyết
        if (event.rating() <= 2) {
            List<User> admins = userRepository.findAllAdmins();
            admins.forEach(admin ->
                    {
                        assert tutor != null;
                        notificationService.send(
                                admin,
                                "negative_review_alert",
                                "Cảnh báo đánh giá thấp",
                                "Gia sư " + tutor.getFullName() + " vừa nhận 1 đánh giá " + event.rating() + " sao. Hãy kiểm tra ngay.",
                                "{\"redirect\": \"/admin/reviews\"}"
                        );
                    }
            );
        }
    }
}