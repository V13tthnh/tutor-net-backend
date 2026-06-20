package com.tutornet.tutor_net.mail.senders;

import com.tutornet.tutor_net.mail.BaseEmailSender;
import com.tutornet.tutor_net.mail.payload.VerificationPayload;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class VerificationEmailSender extends BaseEmailSender<VerificationPayload> {

    public VerificationEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine) {
        super(mailSender, templateEngine);
    }

    @Override
    protected String getTemplateName() {
        return "verification";
    }

    @Override
    protected String getSubject(VerificationPayload payload) {
        return "TutorNet - Xác thực tài khoản của bạn";
    }

    @Override
    protected void buildContext(Context ctx, VerificationPayload payload) {
        ctx.setVariable("verifyLink", "http://localhost:3000/verify-email?token=" + payload.token());
    }
}
