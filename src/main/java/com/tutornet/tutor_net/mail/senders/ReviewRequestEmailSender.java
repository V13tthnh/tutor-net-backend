package com.tutornet.tutor_net.mail.senders;

import com.tutornet.tutor_net.mail.payload.ReviewEmailPayload;
import com.tutornet.tutor_net.mail.BaseEmailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ReviewRequestEmailSender extends BaseEmailSender<ReviewEmailPayload> {

    public ReviewRequestEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine) {
        super(mailSender, templateEngine);
    }

    @Override
    protected String getTemplateName() {
        return "review-request";
    }

    @Override
    protected String getSubject(ReviewEmailPayload data) {
        return "TutorNet - Khóa học hoàn tất! Hãy để lại đánh giá cho gia sư " + data.tutorName();
    }

    @Override
    protected void buildContext(Context ctx, ReviewEmailPayload data) {
        ctx.setVariable("studentName", data.studentName());
        ctx.setVariable("tutorName", data.tutorName());
        ctx.setVariable("reviewLink", data.reviewLink());
    }
}