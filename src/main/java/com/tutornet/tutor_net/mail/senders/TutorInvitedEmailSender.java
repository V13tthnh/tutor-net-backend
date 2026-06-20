package com.tutornet.tutor_net.mail.senders;

import com.tutornet.tutor_net.mail.BaseEmailSender;
import com.tutornet.tutor_net.mail.payload.TutorInvitedPayload;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class TutorInvitedEmailSender extends BaseEmailSender<TutorInvitedPayload> {
    public TutorInvitedEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine) { super(mailSender, templateEngine); }

    @Override protected String getTemplateName() { return "tutor-invited"; }
    @Override protected String getSubject(TutorInvitedPayload p) { return "TutorNet - Bạn nhận được một lời mời dạy học mới!"; }
    @Override protected void buildContext(Context ctx, TutorInvitedPayload p) {
        ctx.setVariable("tutorName", p.tutorName());
        ctx.setVariable("studentName", p.studentName());
        ctx.setVariable("studentMessage", p.message());
    }
}