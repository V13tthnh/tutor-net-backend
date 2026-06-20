package com.tutornet.tutor_net.mail.senders;

import com.tutornet.tutor_net.mail.BaseEmailSender;
import com.tutornet.tutor_net.mail.payload.StudentTutorPayload;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class TutorAcceptedInvitationEmailSender extends BaseEmailSender<StudentTutorPayload> {
    public TutorAcceptedInvitationEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine) { super(mailSender, templateEngine); }

    @Override protected String getTemplateName() { return "tutor-accepted-invitation"; }
    @Override protected String getSubject(StudentTutorPayload p) { return "TutorNet - Gia sư đã đồng ý nhận lớp của bạn!"; }
    @Override protected void buildContext(Context ctx, StudentTutorPayload p) {
        ctx.setVariable("studentName", p.studentName());
        ctx.setVariable("tutorName", p.tutorName());
    }
}
