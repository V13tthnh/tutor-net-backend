package com.tutornet.tutor_net.service.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tutornet.tutor_net.dto.request.ContractDisputeRequest;
import com.tutornet.tutor_net.dto.response.AdminContractResponse;
import com.tutornet.tutor_net.dto.response.ContractResponse;
import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.Contract;
import com.tutornet.tutor_net.enums.ContractStatus;
import com.tutornet.tutor_net.event.ContractSignedEvent;
import com.tutornet.tutor_net.exception.BadRequestException;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.mapper.ContractMapper;
import com.tutornet.tutor_net.repository.ClassRequestRepository;
import com.tutornet.tutor_net.repository.ContractRepository;
import com.tutornet.tutor_net.service.ContractService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ClassRequestRepository classRequestRepo;
    private final TemplateEngine templateEngine;
    private final FileStorageServiceImpl fileStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final ContractMapper contractMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ContractResponse> getMyContracts(Long userId, String keyword, ContractStatus status, Pageable pageable) {

        // 1. Chuẩn hóa từ khóa và gán cờ nhận diện
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        boolean hasKeyword = (cleanKeyword != null);
        boolean hasStatus = (status != null);

        // 2. Truyền chuỗi rỗng "" thay vì truyền null để tránh lỗi hàm CONCAT trong Postgres
        String safeKeyword = hasKeyword ? cleanKeyword : "";

        // 3. Gọi DB với các cờ báo hiệu
        Page<Contract> contractPage = contractRepository.searchMyContracts(
                userId,
                safeKeyword,
                hasKeyword,
                status,
                hasStatus,
                pageable
        );

        // 4. Map sang DTO trả về cho Frontend
        return contractPage.map(contract -> contractMapper.toResponse(contract, userId));
    }

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
        return contractMapper.toResponse(savedContract);
    }



    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss 'ngày' dd 'tháng' MM 'năm' yyyy");

    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void signContractAndGeneratePdf(Long contractId, String ipAddress) {

        // ── Load & validate ─────────────────────────────────────────
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng #" + contractId));

        if (contract.getStatus() != ContractStatus.DRAFT
                && contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new BadRequestException("Hợp đồng #" + contractId + " đang ở trạng thái " + contract.getStatus() + ", không thể ký.");
        }

        // ── Bước 1: Khởi tạo IP + timestamp ─────────────────────────
        LocalDateTime signedAt = LocalDateTime.now();

        // ── Bước 2: Thymeleaf Context (hash tạm để render lần 1) ────
        Context ctx = buildThymeleafContext(contract, ipAddress, signedAt, "ĐANG XỬ LÝ...");

        // ── Bước 3: Render lần 1 → tính SHA-256 ─────────────────────
        byte[] pdfBytes = renderPdf(templateEngine.process("email/contracts", ctx));
        String sha256Hash = sha256Hex(pdfBytes);

        // ── Render lần 2: ghi hash thật vào evidence-block ──────────
        ctx.setVariable("contractHash", sha256Hash);
        pdfBytes = renderPdf(templateEngine.process("email/contracts", ctx));

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

        eventPublisher.publishEvent(new ContractSignedEvent(
                contract.getContractNumber(),
                contract.getTutor().getUser().getEmail(),
                contract.getTutor().getUser().getFullName(),
                contract.getClassRequest().getContactEmail(),
                contract.getClassRequest().getContactName(),
                pdfBytes // Truyền mảng bytes thu được từ lệnh renderPdf trước đó sang
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminContractResponse> getContractsForAdmin(String keyword, ContractStatus status, Boolean isFeePaid, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        boolean hasKeyword = (cleanKeyword != null);
        boolean hasStatus = (status != null);
        boolean hasIsFeePaid = (isFeePaid != null);
        String safeKeyword = hasKeyword ? cleanKeyword : "";

        Page<Contract> contracts = contractRepository.findAllForAdmin(
                safeKeyword, hasKeyword, status, hasStatus, isFeePaid, hasIsFeePaid, pageable
        );

        return contracts.map(contractMapper::toAdminResponse);
    }

    @Override
    @Transactional
    public void confirmPaymentByAdmin(Long contractId, Long adminId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng yêu cầu."));

        // Dùng Boolean.TRUE.equals để tránh lỗi NullPointerException nếu isFeePaid bị NULL
        if (Boolean.TRUE.equals(contract.getIsFeePaid())) {
            throw new BusinessException("Hợp đồng này đã được xác nhận đóng phí từ trước.");
        }

        // Cập nhật trạng thái dòng tiền
        contract.setIsFeePaid(true);
        contract.setPaidAt(LocalDateTime.now());

        // Nếu hợp đồng đã được gia sư ký trước đó rồi, tự động kích hoạt trạng thái ACTIVE luôn
        if (contract.getStatus() == ContractStatus.PENDING_SIGNATURE && contract.getSignedAt() != null) {
            contract.setStatus(ContractStatus.ACTIVE);
        }

        contractRepository.save(contract);
        // Đính kèm logic phát event thông báo in-app hoặc gửi mail biên nhận thu tiền tại đây (nếu có)
    }

    @Override
    @Transactional
    public void resolveContractDispute(Long contractId, ContractDisputeRequest request, Long adminId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng xử lý sự cố."));

        if (request.status() != ContractStatus.CANCELLED && request.status() != ContractStatus.VIOLATED) {
            throw new BusinessException("Trạng thái xử lý tranh chấp không hợp lệ. Chỉ chấp nhận CANCELLED hoặc VIOLATED.");
        }

        contract.setStatus(request.status());

        // Tận dụng trường ghi chú hoặc lưu vết lý do
        // Giả sử hệ thống lưu vết log hoặc lưu thẳng vào trường contract_file_url/bảng log riêng.
        // Ở đây ta cập nhật trạng thái đóng gói tài chính:
        if (request.refundFee()) {
            contract.setIsFeePaid(false);
            contract.setPaidAt(null);
        }

        contractRepository.save(contract);
        // Gửi mail thông báo cho cả Gia sư và Phụ huynh về quyết định can thiệp của Ban quản trị
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminContractResponse> getContractsForExport(ContractStatus status, Boolean isFeePaid) {
        boolean hasStatus = (status != null);
        boolean hasIsFeePaid = (isFeePaid != null);

        List<Contract> contracts = contractRepository.findAllForAdminExport(status, hasStatus, isFeePaid, hasIsFeePaid);
        return contracts.stream().map(contractMapper::toAdminResponse).collect(Collectors.toList());
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
            builder.useFont(() -> {
                try {
                    return new ClassPathResource("fonts/times.ttf").getInputStream();
                } catch (IOException e) {
                    throw new RuntimeException("Không tìm thấy file font times-new-roman.ttf", e);
                }
            }, "Times New Roman");
            builder.withHtmlContent(htmlContent, "/");
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
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount);
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

