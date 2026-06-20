package com.tutornet.tutor_net.mail.senders;

import com.tutornet.tutor_net.mail.BaseEmailSender;
import com.tutornet.tutor_net.mail.payload.TutorReviewMailPayload;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class TutorApprovedEmailSender extends BaseEmailSender<TutorReviewMailPayload> {
    public TutorApprovedEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine) { super(mailSender, templateEngine); }

    @Override protected String getTemplateName() { return "tutor-approved"; }
    @Override protected String getSubject(TutorReviewMailPayload p) { return "TutorNet - Hồ sơ gia sư của bạn đã được duyệt"; }
    @Override
    protected void buildContext(Context ctx, TutorReviewMailPayload p) {
        ctx.setVariable("fullName", p.fullName());
    }
}
