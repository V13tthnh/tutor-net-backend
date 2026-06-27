package com.tutornet.tutor_net.mail.senders;

import com.tutornet.tutor_net.mail.BaseEmailSender;
import com.tutornet.tutor_net.mail.payload.StudentTutorPayload;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class TutorAppliedEmailSender extends BaseEmailSender<StudentTutorPayload> {
    public TutorAppliedEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine) { super(mailSender, templateEngine); }
    @Override protected String getTemplateName() { return "tutor-applied"; }
    @Override protected String getSubject(StudentTutorPayload p) { return "TutorNet - Có gia sư mới ứng tuyển vào lớp của bạn!"; }
    @Override protected void buildContext(Context ctx, StudentTutorPayload p) {
        ctx.setVariable("tutorName", p.tutorName());
        ctx.setVariable("studentName", p.studentName());
        ctx.setVariable("isGuest", p.isGuest());
        ctx.setVariable("actionUrl", p.actionUrl());
    }
}