package com.tutornet.tutor_net.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
public abstract class BaseEmailSender<T> {

    protected final JavaMailSender mailSender;
    protected final TemplateEngine templateEngine;

    protected BaseEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    // TEMPLATE METHOD: Quy trình chuẩn để gửi 1 email
    public void execute(String toEmail, T dataPayload) {
        try {
            log.info("Bắt đầu chuẩn bị email [{}] gửi tới: {}", getTemplateName(), toEmail);

            // 1. Chuẩn bị biến (variables) cho Thymeleaf
            Context ctx = new Context();
            buildContext(ctx, dataPayload);

            // 2. Render HTML
            String htmlContent = templateEngine.process("email/" + getTemplateName(), ctx);

            // 3. Cấu hình thư
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@tutornet.com");
            helper.setTo(toEmail);
            helper.setSubject(getSubject(dataPayload));
            helper.setText(htmlContent, true);

            // (Tuỳ chọn) Nếu có file đính kèm, lớp con sẽ ghi đè hàm này
            addAttachments(helper, dataPayload);

            // 4. Gửi
            mailSender.send(message);
            log.info("Đã gửi thành công email [{}] tới: {}", getTemplateName(), toEmail);

        } catch (MessagingException e) {
            log.error("Lỗi gửi email [{}] tới {}: {}", getTemplateName(), toEmail, e.getMessage());
        }
    }

    // --- Các bước trừu tượng lớp con phải triển khai ---
    protected abstract String getTemplateName();
    protected abstract String getSubject(T dataPayload);
    protected abstract void buildContext(Context ctx, T dataPayload);

    // --- Hook Method (Phương thức móc nối - Có thể ghi đè hoặc bỏ qua) ---
    protected void addAttachments(MimeMessageHelper helper, T dataPayload) throws MessagingException {
        // Mặc định không làm gì. Nếu email nào cần đính kèm file (như Hợp đồng) thì sẽ Override hàm này.
    }
}
