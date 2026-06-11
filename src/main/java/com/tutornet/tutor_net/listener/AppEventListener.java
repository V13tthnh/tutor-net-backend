package com.tutornet.tutor_net.listener;

import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.event.*;
import com.tutornet.tutor_net.repository.UserRepository;
import com.tutornet.tutor_net.service.ContractService;
import com.tutornet.tutor_net.service.MailService;
import com.tutornet.tutor_net.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppEventListener {

    private final MailService mailService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ContractService contractService;

    // ── Auth events ──

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Gửi mail xác thực tới {}", event.email());
        mailService.sendVerificationEmail(event.email(), event.verificationToken());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Gửi mail reset password tới {}", event.email());
        mailService.sendPasswordResetEmail(event.email(), event.fullName(), event.resetToken());
    }

    // ── Tutor review events ──

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTutorReviewed(TutorReviewedEvent event) {
        switch (event.newStatus()) {
            case APPROVED -> {
                mailService.sendTutorApprovedEmail(event.tutorEmail(), event.tutorFullName());
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
                mailService.sendTutorRejectedEmail(event.tutorEmail(), event.tutorFullName(), event.rejectionReason());
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
            // Mời đích danh: Gửi Notif cho Gia sư
            log.info("Lưu DB thành công. Bắn thông báo Notif cho gia sư: {}", event.targetTutorUser().getEmail());

            notificationService.send(
                    event.targetTutorUser(),
                    "class_request_direct",
                    "Bạn có lời mời dạy mới \uD83C\uDF89",
                    "Có học viên vừa gửi lời mời bạn dạy môn " + event.subjectName(),
                    "{\"redirect\": \"/tutor/class-requests/" + event.classRequestId() + "\"}"
            );

        } else {
            // Đăng công khai: Gửi Notif cho toàn bộ Admin
            log.info("Lưu DB thành công. Bắn thông báo Notif (Public Request) cho Admin");

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

            // 1. Gửi thông báo email và in-app cho học viên (Logic cũ đã viết)
            if (event.studentUser() != null) {
                notificationService.send(
                        event.studentUser(),
                        "invite_accepted",
                        "Gia sư đã đồng ý nhận lớp! 🎉",
                        "Gia sư " + event.tutorName() + " đã đồng ý yêu cầu dạy học của bạn.",
                        "{\"redirect\": \"/student/classes/" + event.classRequestId() + "\"}"
                );
            }

            // THỰC HIỆN TODO: TỰ ĐỘNG KÍCH HOẠT SINH HỢP ĐỒNG NHÁP TẠI ĐÂY
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
        // 1. Xác định tiêu đề và nội dung thông báo dựa trên kết quả duyệt
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
        // Nếu học viên có tài khoản, gửi Notification trong App
        if (event.studentUser() != null) {
            notificationService.send(event.studentUser(), "new_application",
                    "Có gia sư ứng tuyển!",
                    "Gia sư " + event.tutorName() + " vừa ứng tuyển vào lớp của bạn.",
                    "{\"redirect\": \"/student/requests/" + event.classRequestId() + "\"}");
        }

        // NẾU LÀ VÃNG LAI: Gửi email (Dùng email đã lấy từ event)
        if (event.studentEmail() != null && !event.studentEmail().isBlank()) {
            mailService.sendTutorAppliedEmail(
                    event.studentEmail(),
                    event.studentName(),
                    event.tutorName()
            );
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

        // 2. Gửi email cho gia sư
        mailService.sendTutorInvitedEmail(
                event.tutorEmail(),
                event.tutorName(),
                event.studentName(),
                event.message()
        );
    }

    /**
     * Gia sư chấp nhận lời mời → tạo Contract DRAFT + gửi mail/notif cho học viên.
     * Chạy SAU KHI transaction commit (AFTER_COMMIT) để tránh tạo Contract
     * khi DB rollback giữa chừng.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTutorAcceptedInvitation(TutorAcceptedInvitationEvent event) {

        log.info("Gia sư {} đã chấp nhận invitation #{}, classRequest #{}",
                event.tutorName(), event.invitationId(), event.classRequestId());

        // 1. Gửi thông báo in-app cho học viên (nếu có tài khoản)
        if (event.studentUser() != null) {
            notificationService.send(
                    event.studentUser(),
                    "invite_accepted",
                    "Gia sư đã đồng ý nhận lớp! 🎉",
                    "Gia sư " + event.tutorName() + " đã chấp nhận lời mời của bạn.",
                    "{\"redirect\": \"/student/classes/" + event.classRequestId() + "\"}"
            );
        }

        // 2. Gửi email thông báo cho học viên (cả khách vãng lai)
        if (event.studentEmail() != null && !event.studentEmail().isBlank()) {
            mailService.sendTutorAcceptedInvitationEmail(
                    event.studentEmail(),
                    event.studentName(),
                    event.tutorName()
            );
        }

        // 3. Tự động tạo Contract DRAFT — dùng try-catch riêng để lỗi Contract
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
}