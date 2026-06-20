package com.tutornet.tutor_net.mail.senders;

import com.tutornet.tutor_net.mail.BaseEmailSender;
import com.tutornet.tutor_net.mail.payload.PaymentReminderPayload;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class PaymentReminderEmailSender extends BaseEmailSender<PaymentReminderPayload> {

    public PaymentReminderEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine) {
        super(mailSender, templateEngine);
    }

    @Override
    protected String getTemplateName() { return "payment-reminder"; }

    @Override
    protected String getSubject(PaymentReminderPayload p) {
        return "TutorNet - Nhắc nhở thanh toán phí nhận lớp (Hợp đồng " + p.contractNumber() + ")";
    }

    @Override
    protected void buildContext(Context ctx, PaymentReminderPayload p) {
        ctx.setVariable("name", p.name());
        ctx.setVariable("contractNumber", p.contractNumber());

        // Format tiền VNĐ
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        ctx.setVariable("amount", format.format(p.amount()));

        // Format ngày tháng
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        ctx.setVariable("deadline", p.deadline().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(dateFormatter));
    }
}
