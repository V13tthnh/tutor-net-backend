package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    @Async("mailExecutor")
    public void sendVerificationEmail(String toEmail, String token) {
        log.info("Gửi mail xác thực tới {}", toEmail);
        Context ctx = new Context();
        ctx.setVariable("verifyLink", "http://localhost:3000/verify-email?token=" + token);
        sendHtmlEmail(toEmail, "TutorNet - Xác thực tài khoản của bạn", "verification", ctx);
    }

    @Override
    @Async("mailExecutor")
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        log.info("Gửi mail reset password tới {}", toEmail);
        Context ctx = new Context();
        ctx.setVariable("fullName", fullName);
        ctx.setVariable("resetLink", "http://localhost:3000/reset-password?token=" + token);
        sendHtmlEmail(toEmail, "TutorNet - Đặt lại mật khẩu", "password-reset", ctx);
    }

    @Override
    @Async("mailExecutor")
    public void sendTutorApprovedEmail(String toEmail, String fullName) {
        Context ctx = new Context();
        ctx.setVariable("fullName", fullName);
        sendHtmlEmail(toEmail, "TutorNet - Hồ sơ gia sư của bạn đã được duyệt ", "tutor-approved", ctx);
    }

    @Override
    @Async("mailExecutor")
    public void sendTutorRejectedEmail(String toEmail, String fullName, String reason) {
        Context ctx = new Context();
        ctx.setVariable("fullName", fullName);
        ctx.setVariable("reason", reason);
        sendHtmlEmail(toEmail, "TutorNet - Hồ sơ gia sư của bạn chưa được duyệt", "tutor-rejected", ctx);
    }

    @Override
    @Async("mailExecutor")
    public void sendTutorInvitedEmail(String toEmail, String tutorName,
                                      String studentName, String studentMessage) {
        log.info("Gửi mail lời mời dạy học tới {}", toEmail);
        Context ctx = new Context();
        ctx.setVariable("tutorName", tutorName);
        ctx.setVariable("studentName", studentName);
        ctx.setVariable("studentMessage", studentMessage);
        sendHtmlEmail(toEmail, "TutorNet - Bạn nhận được một lời mời dạy học mới! ", "tutor-invited", ctx);
    }

    @Override
    @Async("mailExecutor")
    public  void sendTutorDirectInviteEmail(String tutorEmail, String tutorName, String subjectName) {
        Context ctx = new Context();
        ctx.setVariable("fullName", tutorEmail);
        ctx.setVariable("email", tutorName);
        ctx.setVariable("subjet", subjectName);
        sendHtmlEmail(tutorEmail, "TutorNet - Hồ sơ gia sư của bạn đã được duyệt", "tutor-approved", ctx);
    }

    @Override
    @Async("mailExecutor")
    public void sendTutorAppliedEmail(String toEmail, String studentName, String tutorName) {
        Context ctx = new Context();
        ctx.setVariable("studentName", studentName);
        ctx.setVariable("tutorName", tutorName);
        sendHtmlEmail(toEmail, "TutorNet - Có gia sư mới ứng tuyển vào lớp của bạn!", "tutor-applied", ctx);
    }

    @Override
    @Async("mailExecutor")
    public void sendClassRequestConfirmationEmail(String toEmail, String studentName, String subjectName) {
        Context ctx = new Context();
        ctx.setVariable("studentName", studentName);
        ctx.setVariable("subjectName", subjectName);
        sendHtmlEmail(toEmail, "TutorNet - Xác nhận yêu cầu tìm gia sư thành công",
                "class-request-confirmation", ctx);
    }

    @Override
    @Async("mailExecutor")
    public void sendClassRequestApprovedEmail(String toEmail, String contactName, String subjectName) {
        Context ctx = new Context();
        ctx.setVariable("contactName", contactName);
        ctx.setVariable("subjectName", subjectName);
        sendHtmlEmail(toEmail, "TutorNet - Yêu cầu tìm gia sư của bạn đã được duyệt ",
                "class-request-approved", ctx);
    }

    @Override
    @Async("mailExecutor")
    public void sendClassRequestRejectedEmail(String toEmail, String contactName,
                                              String subjectName, String rejectionReason) {
        Context ctx = new Context();
        ctx.setVariable("contactName", contactName);
        ctx.setVariable("subjectName", subjectName);
        ctx.setVariable("rejectionReason", rejectionReason);
        sendHtmlEmail(toEmail, "TutorNet - Yêu cầu tìm gia sư của bạn chưa được duyệt",
                "class-request-rejected", ctx);
    }

    @Override
    @Async("mailExecutor")
    public void sendTutorAcceptedInvitationEmail(String toEmail,
                                                 String studentName,
                                                 String tutorName) {
        log.info("Gửi mail thông báo gia sư chấp nhận lời mời tới {}", toEmail);
        Context ctx = new Context();
        ctx.setVariable("studentName", studentName);
        ctx.setVariable("tutorName",   tutorName);
        sendHtmlEmail(
                toEmail,
                "TutorNet - Gia sư đã đồng ý nhận lớp của bạn!",
                "tutor-accepted-invitation",   // → resources/templates/email/tutor-accepted-invitation.html
                ctx
        );
    }

    /**
     * Gửi email đính kèm hợp đồng điện tử định dạng PDF từ mảng bytes dữ liệu.
     *
     * @param toEmail        Email người nhận (Gia sư hoặc Phụ huynh)
     * @param recipientName  Tên người nhận
     * @param contractNumber Mã số hợp đồng (dùng làm tên file đính kèm)
     * @param pdfBytes       Mảng bytes dữ liệu của file PDF hợp đồng
     */
    @Override
    public void sendContractAttachmentEmail(String toEmail, String recipientName, String contractNumber, byte[] pdfBytes) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[TutorNet] Xác nhận ký kết hợp đồng điện tử thành công - Mã số " + contractNumber);

            // XỬ LÝ ĐỔ DỮ LIỆU QUA THYMELEAF TEMPLATE
            Context context = new Context();
            context.setVariable("recipientName", recipientName);
            context.setVariable("contractNumber", contractNumber);

            // Biên dịch file html nằm tại templates/emails/contract_email.html
            String htmlBody = templateEngine.process("email/contract_email", context);

            helper.setText(htmlBody, true);

            // Đính kèm tệp tin PDF
            helper.addAttachment(
                    contractNumber + ".pdf",
                    new ByteArrayResource(pdfBytes),
                    "application/pdf"
            );

            mailSender.send(mimeMessage);
            log.info("Email chứa tệp đính kèm hợp đồng {} đã được gửi thành công tới hòm thư: {}", contractNumber, toEmail);

        } catch (MessagingException e) {
            log.error("Lỗi cấu trúc gửi tệp đính kèm email hợp đồng {}: {}", contractNumber, e.getMessage());
            throw new RuntimeException("Không thể gửi email đính kèm hợp đồng: " + e.getMessage(), e);
        }
    }

    @Override
    @Async("mailExecutor")
    public void sendTutorApplicationAcceptedEmail(String toEmail, String tutorName, String studentName) {
        log.info("Gửi mail thông báo gia sư được chọn tới {}", toEmail);
        Context ctx = new Context();
        ctx.setVariable("tutorName", tutorName);
        ctx.setVariable("studentName", studentName);
        sendHtmlEmail(
                toEmail,
                "TutorNet - Chúc mừng! Bạn đã được chọn để nhận lớp",
                "tutor-application-accepted",
                ctx
        );
    }

    private void sendHtmlEmail(String toEmail, String subject, String templateName,
                               org.thymeleaf.context.Context ctx) {
        try {
            String htmlContent = templateEngine.process("email/" + templateName, ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@tutornet.com");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Lỗi gửi mail [{}] tới {}: {}", templateName, toEmail, e.getMessage());
        }
    }

    @Override
    @Async("mailExecutor")
    public void sendApplicationRejectedByAdminEmail(String toEmail, String tutorName, String contactName) {
        log.info("Gửi mail thông báo đơn bị ẩn tới {}", toEmail);
        Context ctx = new Context();
        ctx.setVariable("tutorName", tutorName);
        ctx.setVariable("contactName", contactName);
        sendHtmlEmail(
                toEmail,
                "TutorNet - Đơn ứng tuyển của bạn không được chấp thuận",
                "application-rejected-by-admin",
                ctx
        );
    }

    @Override
    public void sendPaymentReminderEmail(String to, String name, String contractNumber, BigDecimal amount, Instant deadline) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("contractNumber", contractNumber);

            // Format tiền tệ VNĐ
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            context.setVariable("amount", format.format(amount));

            // Format ngày tháng
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            context.setVariable(
                    "deadline",
                    deadline.atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                            .format(dateFormatter)
            );

            // Cần tạo 1 file payment-reminder.html trong thư mục resources/templates/email
            String html = templateEngine.process("email/payment-reminder", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("TutorNet - Nhắc nhở thanh toán phí nhận lớp (Hợp đồng " + contractNumber + ")");
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email nhắc nợ thanh toán đến {}: {}", to, e.getMessage());
        }
    }

    @Override
    @Async("mailExecutor")
    public void sendReviewRequestEmail(String toEmail, String studentName, String tutorName, String reviewLink) {
        log.info("Gửi mail yêu cầu đánh giá gia sư tới {}", toEmail);

        Context ctx = new Context();
        ctx.setVariable("studentName", studentName);
        ctx.setVariable("tutorName", tutorName);
        ctx.setVariable("reviewLink", reviewLink); // Đổ link chứa Token vào nút bấm

        sendHtmlEmail(
                toEmail,
                "TutorNet - Khóa học hoàn tất! Hãy để lại đánh giá cho gia sư của bạn",
                "review-request", // Sẽ map với file: resources/templates/email/review-request.html
                ctx
        );
    }
}