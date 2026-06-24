package com.tutornet.tutor_net.mail.senders;

import com.tutornet.tutor_net.config.VNPayConfig;
import com.tutornet.tutor_net.mail.BaseEmailSender;
import com.tutornet.tutor_net.mail.payload.ContractAttachmentPayload;
import jakarta.mail.MessagingException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ContractAttachmentEmailSender extends BaseEmailSender<ContractAttachmentPayload> {

    private final VNPayConfig vnPayConfig;

    public ContractAttachmentEmailSender(JavaMailSender mailSender, TemplateEngine templateEngine, VNPayConfig vnPayConfig) {
        super(mailSender, templateEngine);
        this.vnPayConfig = vnPayConfig;
    }

    @Override
    protected String getTemplateName() { return "contract_email"; } // File html: contract_email.html

    @Override
    protected String getSubject(ContractAttachmentPayload p) {
        return "[TutorNet] Xác nhận ký kết hợp đồng điện tử thành công - Mã số " + p.contractNumber();
    }

    @Override
    protected void buildContext(Context ctx, ContractAttachmentPayload p) {
        ctx.setVariable("recipientName", p.recipientName());
        ctx.setVariable("contractNumber", p.contractNumber());
        ctx.setVariable("isTutor", p.isTutor());
        
        if (p.isTutor()) {
            String returnUrl = vnPayConfig.vnp_ReturnUrl;
            String basePaymentUrl = returnUrl.substring(0, returnUrl.lastIndexOf("/"));
            String paymentLink = basePaymentUrl + "/click-pay-email?contractNumber=" + p.contractNumber();
            ctx.setVariable("paymentLink", paymentLink);
        }
    }

    // GHI ĐÈ HOOK METHOD ĐỂ ĐÍNH KÈM FILE
    @Override
    protected void addAttachments(MimeMessageHelper helper, ContractAttachmentPayload p) throws MessagingException {
        helper.addAttachment(
                p.contractNumber() + ".pdf",
                new ByteArrayResource(p.pdfBytes()),
                "application/pdf"
        );
    }
}
