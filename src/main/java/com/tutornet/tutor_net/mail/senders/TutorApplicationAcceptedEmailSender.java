package com.tutornet.tutor_net.mail.senders;

import com.tutornet.tutor_net.mail.BaseEmailSender;
import com.tutornet.tutor_net.mail.payload.StudentTutorPayload;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class TutorApplicationAcceptedEmailSender extends BaseEmailSender<StudentTutorPayload> {
    public TutorApplicationAcceptedEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine) { super(mailSender, templateEngine); }
    @Override protected String getTemplateName() { return "tutor-application-accepted"; }
    @Override protected String getSubject(StudentTutorPayload p) { return "TutorNet - Chúc mừng! Bạn đã được chọn để nhận lớp"; }
    @Override protected void buildContext(Context ctx, StudentTutorPayload p) {
        ctx.setVariable("tutorName", p.tutorName());
        ctx.setVariable("studentName", p.studentName());
    }
}
