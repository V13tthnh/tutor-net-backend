package com.tutornet.tutor_net.export.pdf;

import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.Contract;
import com.tutornet.tutor_net.export.BasePdfGenerator;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class ContractPdfGenerator extends BasePdfGenerator<ContractPdfPayload> {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss 'ngày' dd 'tháng' MM 'năm' yyyy");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("'Ngày' d 'tháng' M 'năm' yyyy");

    public ContractPdfGenerator(TemplateEngine templateEngine) {
        super(templateEngine);
    }

    @Override
    protected String getTemplateName() {
        return "contracts"; // Trỏ tới templates/contract/contracts.html
    }

    // Hàm hỗ trợ Render 2 lần để lấy mã Hash
    public byte[] generateSignedPdfBytes(Contract contract, String ipAddress, Instant signedAt) {
        // Render với Hash tạm
        ContractPdfPayload payloadPass1 = new ContractPdfPayload(contract, ipAddress, signedAt, "ĐANG XỬ LÝ...");
        byte[] firstPassBytes = generatePdfBytes(payloadPass1);

        // Băm SHA-256 và Render bản chính thức
        String realHash = sha256Hex(firstPassBytes);
        ContractPdfPayload payloadPass2 = new ContractPdfPayload(contract, ipAddress, signedAt, realHash);

        return generatePdfBytes(payloadPass2);
    }

    @Override
    protected void buildContext(Context ctx, ContractPdfPayload payload) {
        Contract contract = payload.contract();
        ClassRequest cr = contract.getClassRequest();
        var tutorUser = contract.getTutor().getUser();

        ctx.setVariable("contractDate", payload.signedAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(DATE_FORMAT));

        // Bên B – Gia sư
        ctx.setVariable("tutorName", tutorUser.getFullName().toUpperCase());
        ctx.setVariable("tutorBirthYear", tutorUser.getBirthYear() != null ? tutorUser.getBirthYear() : "N/A");
        ctx.setVariable("tutorPhone", tutorUser.getPhone());
        ctx.setVariable("tutorEmail", tutorUser.getEmail());

        // Bên C – Học viên / Phụ huynh
        ctx.setVariable("studentName", cr.getContactName().toUpperCase());
        ctx.setVariable("studentPhone", cr.getContactPhone());
        ctx.setVariable("studentEmail", cr.getContactEmail() != null ? cr.getContactEmail() : "Không có");
        ctx.setVariable("studentAddress", cr.getAddressDetail() != null ? cr.getAddressDetail() : "Học online");

        // Thông tin lớp học
        ctx.setVariable("classCode", contract.getContractNumber());
        ctx.setVariable("subjectAndLevel", cr.getSubject() != null ? cr.getSubject().getName() + " - " + cr.getGradeLevel() : cr.getGradeLevel());
        ctx.setVariable("tuitionRate", formatCurrency(cr.getHourlyRate() != null ? cr.getHourlyRate() : cr.getProposedPrice()));
        ctx.setVariable("scheduleDetail", buildScheduleDetail(cr));
        ctx.setVariable("introductionFee", formatCurrency(contract.getIntroductionFee()));

        // Evidence block
        ctx.setVariable("ipAddress", payload.ipAddress());
        ctx.setVariable("signedAt", payload.signedAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(DATETIME_FORMAT));
        ctx.setVariable("contractHash", payload.contractHash());
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính SHA-256: " + e.getMessage(), e);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount);
    }

    private String buildScheduleDetail(ClassRequest cr) {
        StringBuilder sb = new StringBuilder();
        if (cr.getSessionsPerWeek() != null) sb.append(cr.getSessionsPerWeek()).append(" buổi / tuần");
        if (cr.getDurationMinutes() != null) sb.append(" – Mỗi buổi ").append(cr.getDurationMinutes()).append(" phút");
        return sb.isEmpty() ? "Theo thỏa thuận" : sb.toString();
    }
}