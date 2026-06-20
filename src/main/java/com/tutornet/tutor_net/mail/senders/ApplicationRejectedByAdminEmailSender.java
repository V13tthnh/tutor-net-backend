package com.tutornet.tutor_net.mail.senders;

import com.tutornet.tutor_net.mail.BaseEmailSender;
import com.tutornet.tutor_net.mail.payload.ApplicationRejectedPayload;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ApplicationRejectedByAdminEmailSender extends BaseEmailSender<ApplicationRejectedPayload> {
    public ApplicationRejectedByAdminEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine) { super(mailSender, templateEngine); }

    @Override protected String getTemplateName() { return "application-rejected-by-admin"; }
    @Override protected String getSubject(ApplicationRejectedPayload p) { return "TutorNet - Đơn ứng tuyển của bạn không được chấp thuận"; }
    @Override protected void buildContext(Context ctx, ApplicationRejectedPayload p) {
        ctx.setVariable("tutorName", p.tutorName());
        ctx.setVariable("contactName", p.contactName());
    }
}