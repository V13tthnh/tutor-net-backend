package com.tutornet.tutor_net.service.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tutornet.tutor_net.dto.response.ContractResponse;
import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.Contract;
import com.tutornet.tutor_net.enums.ContractStatus;
import com.tutornet.tutor_net.exception.BadRequestException;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.repository.ClassRequestRepository;
import com.tutornet.tutor_net.repository.ContractRepository;
import com.tutornet.tutor_net.service.ContractService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ClassRequestRepository classRequestRepo;
    private final TemplateEngine templateEngine;
    private final FileStorageServiceImpl fileStorageService;

    @Override
    @Transactional
    public ContractResponse createDraftContract(Long requestId) {

        // 1. Kiểm tra xem hợp đồng của lớp này đã tồn tại chưa để đảm bảo tính Idempotency
        if (contractRepository.existsByClassRequestId(requestId)) {
            throw new BusinessException("Hợp đồng cho yêu cầu lớp học này đã được khởi tạo trước đó.");
        }

        // 2. Lấy thông tin chi tiết của lớp học
        ClassRequest classRequest = classRequestRepo.findById(requestId)
                .orElseThrow(() -> ResourceNotFoundException.of("Yêu cầu lớp học", requestId));

        // 3. Tự động sinh mã hợp đồng pháp lý độc nhất (Ví dụ cấu trúc: HD-2026-[Chuỗi ngẫu nhiên])
        String contractNumber = "HD-" + LocalDate.now().getYear() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 4. Công thức tính toán Phí Giao Lớp tự động (Snapshot doanh thu cho nền tảng)
        // Giả định trung bình 1 lớp học 1 tháng gồm 8 buổi, mỗi buổi 2 giờ. Trung tâm thu phí dịch vụ 40% tháng đầu.
        BigDecimal hourlyRate = classRequest.getProposedPrice() != null ? classRequest.getProposedPrice() : BigDecimal.ZERO;
        BigDecimal estimatedMonthlyTuition = hourlyRate.multiply(BigDecimal.valueOf(2 * 8));
        BigDecimal introductionFee = estimatedMonthlyTuition.multiply(BigDecimal.valueOf(0.40));

        // 5. Cấu hình các mốc thời gian dựa theo đúng văn bản cam kết pháp lý
        LocalDate effectiveDate = LocalDate.now(); // Ngày bàn giao lớp học
        LocalDate feePaymentDeadline = effectiveDate.plusDays(35); // Hạn đóng tiền muộn nhất 35 ngày

        // 6. Xây dựng thực thể Contract
        Contract contract = Contract.builder()
                .contractNumber(contractNumber)
                .classRequest(classRequest)
                .tutor(classRequest.getTargetTutor()) // Đã được xác thực quyền sở hữu ở bước phản hồi
                .introductionFee(introductionFee)
                .effectiveDate(effectiveDate)
                .feePaymentDeadline(feePaymentDeadline)
                .status(ContractStatus.DRAFT) // Hợp đồng ở trạng thái sơ thảo, chờ gia sư in và ký tay
                .freeTrialCount(1) // Mặc định tuân thủ chính sách dạy thử 1 buổi đầu miễn phí
                .build();

        Contract savedContract = contractRepository.save(contract);

        // 7. Chuyển đổi dữ liệu sang DTO Response để trả về
        return new ContractResponse(
                savedContract.getId(),
                savedContract.getContractNumber(),
                classRequest.getId(),
                classRequest.getContactName(),
                classRequest.getContactPhone(),
                classRequest.getTargetTutor().getId(),
                classRequest.getTargetTutor().getUser().getFullName(),
                savedContract.getIntroductionFee(),
                savedContract.getEffectiveDate(),
                savedContract.getFeePaymentDeadline(),
                savedContract.getStatus(),
                savedContract.getContractFileUrl(),
                savedContract.getFreeTrialCount(),
                savedContract.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public void processClickwrapSigning(Long contractId, String ipAddress) {
        // 1. Tìm hợp đồng nháp vừa được sinh ra khi Gia sư nhận lớp
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu hợp đồng"));

        // 2. Cập nhật bằng chứng số xác nhận ký Clickwrap
        contract.setStatus(ContractStatus.ACTIVE); // Duyệt thẳng, bỏ qua bước chờ admin check chữ ký tay
        contract.setIpAddress(ipAddress);
        contract.setSignedAt(LocalDateTime.now());
        contractRepository.save(contract);

        // 3. Chuẩn bị dữ liệu để truyền vào file HTML mẫu (Thymeleaf Context)
        Context context = new Context();
        context.setVariable("tutorName", contract.getClassRequest().getUser().getFullName());
        context.setVariable("studentName", contract.getClassRequest().getUser().getFullName());
        context.setVariable("hourlyRate", contract.getClassRequest().getHourlyRate());
        context.setVariable("ipAddress", ipAddress);
        context.setVariable("signedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));

        // 4. Đổ dữ liệu vào 2 file HTML template nằm trong mục resources/templates
        String platformHtml = templateEngine.process("contracts/platform_contract_template", context);
        String classHtml = templateEngine.process("contracts/class_contract_template", context);

        // 5. Biên dịch chuỗi HTML thành các mảng bytes PDF
        byte[] platformPdfBytes = generatePdfBytes(platformHtml);
        byte[] classPdfBytes = generatePdfBytes(classHtml);

        // 6. [Tiến trình tiếp theo của bạn]: Lưu mảng bytes thành file gửi lên S3/Local
        // và gọi mailService gửi đính kèm file này cho Gia sư & Học viên.
    }

    private byte[] generatePdfBytes(String htmlContent) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // 🌟 CHỐT CHẶN KHÔNG BỊ LỖI FONT TIẾNG VIỆT:
            // Bạn cần chuẩn bị 1 file font Arial.ttf đặt trong thư mục resources/fonts/
            builder.useFont(new File(getClass().getClassLoader().getResource("fonts/Arial.ttf").toURI()), "Arial");

            builder.withHtmlContent(htmlContent, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi nghiêm trọng trong quá trình xuất file PDF hợp đồng: " + e.getMessage(), e);
        }
    }

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss 'ngày' dd 'tháng' MM 'năm' yyyy");

    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void signContract(Long contractId, HttpServletRequest request) {

        // ── Load & validate ─────────────────────────────────────────
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hợp đồng #" + contractId));

        if (contract.getStatus() != ContractStatus.DRAFT
                && contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new BadRequestException(
                    "Hợp đồng #" + contractId + " đang ở trạng thái "
                            + contract.getStatus() + ", không thể ký.");
        }

        // ── Bước 2: IP + timestamp ───────────────────────────────────
        String        ipAddress = extractClientIp(request);
        LocalDateTime signedAt  = LocalDateTime.now();

        // ── Bước 1: Thymeleaf Context (hash tạm để render lần 1) ────
        Context ctx = buildThymeleafContext(contract, ipAddress, signedAt, "ĐANG XỬ LÝ...");

        // ── Bước 3: Render lần 1 → tính SHA-256 ─────────────────────
        byte[] pdfBytes  = renderPdf(templateEngine.process("contracts", ctx));
        String sha256Hash = sha256Hex(pdfBytes);

        // ── Render lần 2: ghi hash thật vào evidence-block ──────────
        ctx.setVariable("contractHash", sha256Hash);
        pdfBytes = renderPdf(templateEngine.process("contracts", ctx));

        // ── Bước 4: Lưu file qua FileStorageService (dùng chung) ────
        String fileUrl;
        try {
            fileUrl = fileStorageService.storeContract(contract.getContractNumber(), pdfBytes);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu file hợp đồng: " + e.getMessage(), e);
        }

        // ── Bước 5: Cập nhật Contract → ACTIVE ──────────────────────
        LocalDate today = LocalDate.now();

        contract.setIpAddress(ipAddress);
        contract.setSignedAt(signedAt);
        contract.setContractFileUrl(fileUrl);
        contract.setEffectiveDate(today);
        contract.setFeePaymentDeadline(today.plusDays(35));
        contract.setStatus(ContractStatus.ACTIVE);

        contractRepository.save(contract);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private Context buildThymeleafContext(Contract contract,
                                          String ipAddress,
                                          LocalDateTime signedAt,
                                          String contractHash) {
        ClassRequest cr = contract.getClassRequest();
        Context ctx = new Context();

        // Header
        ctx.setVariable("contractDate",
                "Ngày " + signedAt.getDayOfMonth()
                        + " tháng " + signedAt.getMonthValue()
                        + " năm " + signedAt.getYear());

        // Bên B – Gia sư
        var tutorUser = contract.getTutor().getUser();
        ctx.setVariable("tutorName",      tutorUser.getFullName().toUpperCase());
        ctx.setVariable("tutorBirthYear",
                tutorUser.getBirthYear() != null ? tutorUser.getBirthYear() : "N/A");
        ctx.setVariable("tutorPhone",     tutorUser.getPhone());
        ctx.setVariable("tutorEmail",     tutorUser.getEmail());

        // Bên C – Học viên / Phụ huynh
        ctx.setVariable("studentName",    cr.getContactName().toUpperCase());
        ctx.setVariable("studentPhone",   cr.getContactPhone());
        ctx.setVariable("studentEmail",
                cr.getContactEmail() != null ? cr.getContactEmail() : "Không có");
        ctx.setVariable("studentAddress",
                cr.getAddressDetail() != null ? cr.getAddressDetail() : "Học online");

        // Thông tin lớp học
        ctx.setVariable("classCode",      contract.getContractNumber());
        ctx.setVariable("subjectAndLevel",
                cr.getSubject() != null
                        ? cr.getSubject().getName() + " - " + cr.getGradeLevel()
                        : cr.getGradeLevel());
        ctx.setVariable("tuitionRate",
                formatCurrency(cr.getHourlyRate() != null
                        ? cr.getHourlyRate() : cr.getProposedPrice()));
        ctx.setVariable("scheduleDetail", buildScheduleDetail(cr));

        // Phí môi giới
        ctx.setVariable("introductionFee", formatCurrency(contract.getIntroductionFee()));

        // Evidence block
        ctx.setVariable("ipAddress",     ipAddress);
        ctx.setVariable("signedAt",      signedAt.format(DATETIME_FORMAT));
        ctx.setVariable("contractHash",  contractHash);

        return ctx;
    }

    private byte[] renderPdf(String htmlContent) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi render PDF: " + e.getMessage(), e);
        }
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính SHA-256: " + e.getMessage(), e);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount);
    }

    private String buildScheduleDetail(ClassRequest cr) {
        StringBuilder sb = new StringBuilder();
        if (cr.getSessionsPerWeek() != null)
            sb.append(cr.getSessionsPerWeek()).append(" buổi / tuần");
        if (cr.getDurationMinutes() != null)
            sb.append(" – Mỗi buổi ").append(cr.getDurationMinutes()).append(" phút");
        return sb.isEmpty() ? "Theo thỏa thuận" : sb.toString();
    }
}

