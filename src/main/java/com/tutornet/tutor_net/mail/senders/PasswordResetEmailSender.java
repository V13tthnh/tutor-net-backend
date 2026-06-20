package com.tutornet.tutor_net.mail.senders;

import com.tutornet.tutor_net.mail.BaseEmailSender;
import com.tutornet.tutor_net.mail.payload.PasswordResetPayload;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class PasswordResetEmailSender extends BaseEmailSender<PasswordResetPayload> {
    public PasswordResetEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine) { super(mailSender, templateEngine); }

    @Override protected String getTemplateName() { return "password-reset"; }
    @Override protected String getSubject(PasswordResetPayload p) { return "TutorNet - Đặt lại mật khẩu"; }
    @Override protected void buildContext(Context ctx, PasswordResetPayload p) {
        ctx.setVariable("fullName", p.fullName());
        ctx.setVariable("resetLink", "http://localhost:3000/reset-password?token=" + p.token());
    }
}
