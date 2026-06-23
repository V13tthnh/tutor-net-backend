package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.ContractDisputeRequest;
import com.tutornet.tutor_net.dto.response.AdminContractResponse;
import com.tutornet.tutor_net.dto.response.ContractResponse;
import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.Contract;
import com.tutornet.tutor_net.entity.Transaction;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.enums.ContractStatus;
import com.tutornet.tutor_net.enums.PaymentMethod;
import com.tutornet.tutor_net.enums.TransactionStatus;
import com.tutornet.tutor_net.event.ContractCompletedEvent;
import com.tutornet.tutor_net.event.ContractSignedEvent;
import com.tutornet.tutor_net.exception.BadRequestException;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.export.pdf.ContractPdfGenerator;
import com.tutornet.tutor_net.export.pdf.ContractPdfPayload;
import com.tutornet.tutor_net.mapper.ContractMapper;
import com.tutornet.tutor_net.repository.ClassRequestRepository;
import com.tutornet.tutor_net.repository.ContractRepository;
import com.tutornet.tutor_net.repository.TransactionRepository;
import com.tutornet.tutor_net.service.ContractService;
import com.tutornet.tutor_net.visitor.StandardFeeCalculatorVisitor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ClassRequestRepository classRequestRepo;
    private final TransactionRepository transactionRepository;
    private final TemplateEngine templateEngine;
    private final FileStorageServiceImpl fileStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final ContractMapper contractMapper;
    private final ContractPdfGenerator contractPdfGenerator;

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss 'ngày' dd 'tháng' MM 'năm' yyyy");

    @Override
    @Transactional(readOnly = true)
    public Page<ContractResponse> getMyContracts(Long userId, String keyword, ContractStatus status, Pageable pageable) {

        // Chuẩn hóa từ khóa và gán cờ nhận diện
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        boolean hasKeyword = (cleanKeyword != null);
        boolean hasStatus = (status != null);

        // Truyền chuỗi rỗng "" thay vì truyền null để tránh lỗi hàm CONCAT trong Postgres
        String safeKeyword = hasKeyword ? cleanKeyword : "";

        // Gọi DB với các cờ báo hiệu
        Page<Contract> contractPage = contractRepository.searchMyContracts(
                userId,
                safeKeyword,
                hasKeyword,
                status,
                hasStatus,
                pageable
        );

        // Map sang DTO trả về cho Frontend
        return contractPage.map(contract -> contractMapper.toResponse(contract, userId));
    }

    @Override
    @Transactional
    public ContractResponse createDraftContract(Long requestId) {

        // Kiểm tra xem hợp đồng của lớp này đã tồn tại chưa để đảm bảo tính Idempotency
        if (contractRepository.existsByClassRequestId(requestId)) {
            throw new BusinessException("Hợp đồng cho yêu cầu lớp học này đã được khởi tạo trước đó.");
        }

        // Lấy thông tin chi tiết của lớp học
        ClassRequest classRequest = classRequestRepo.findById(requestId)
                .orElseThrow(() -> ResourceNotFoundException.of("Yêu cầu lớp học", requestId));

        // Tự động sinh mã hợp đồng pháp lý
        String contractNumber = "HD-" + LocalDate.now().getYear() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Tính tổng học phí tháng đầu
        // Giả định trung bình 1 lớp học 1 tháng gồm 8 buổi, mỗi buổi 2 giờ. Trung tâm thu phí dịch vụ 40% tháng đầu.
        BigDecimal hourlyRate = classRequest.getProposedPrice() != null ? classRequest.getProposedPrice() : BigDecimal.ZERO;
        BigDecimal estimatedMonthlyTuition = hourlyRate.multiply(BigDecimal.valueOf(2 * 8));

        // áp dụng visitor pattern để tính phí
        StandardFeeCalculatorVisitor feeVisitor = new StandardFeeCalculatorVisitor(estimatedMonthlyTuition);

        // Cho Visitor đi thăm Lớp học và Gia sư để lấy các chỉ số tính toán
        classRequest.accept(feeVisitor);
        classRequest.getTargetTutor().accept(feeVisitor);

        BigDecimal introductionFee = feeVisitor.getCalculatedFee();

        // Cấu hình các mốc thời gian dựa theo đúng văn bản cam kết pháp lý
        Instant effectiveDate = Instant.now(); // Ngày bàn giao lớp học
        Instant feePaymentDeadline = effectiveDate.plus(35, ChronoUnit.DAYS); // Hạn đóng tiền muộn nhất 35 ngày
        Instant endDate = effectiveDate.plus(30, ChronoUnit.DAYS); // Theo điều 3.3 trong hợp đồng: mốc 100% phí là > 30 ngày

        // Xây dựng thực thể Contract
        Contract contract = Contract.builder()
                .contractNumber(contractNumber)
                .classRequest(classRequest)
                .tutor(classRequest.getTargetTutor())
                .introductionFee(introductionFee)
                .effectiveDate(effectiveDate)
                .feePaymentDeadline(feePaymentDeadline)
                .endDate(endDate)
                .status(ContractStatus.DRAFT)
                .freeTrialCount(1)
                .build();

        Contract savedContract = contractRepository.save(contract);

        return contractMapper.toResponse(savedContract);
    }

    @Override
    @Transactional
    public void signContractAndGeneratePdf(Long contractId, String ipAddress, Long currentUserId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng #" + contractId));

        // Kiểm tra quyền sở hữu IDOR
        if (contract.getTutor() == null || contract.getTutor().getUser() == null ||
                !contract.getTutor().getUser().getId().equals(currentUserId)) {
            throw BusinessException.forbidden("Bạn không có quyền ký hợp đồng này.");
        }

        if (contract.getStatus() != ContractStatus.DRAFT
                && contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new BadRequestException("Hợp đồng #" + contractId + " đang ở trạng thái " + contract.getStatus() + ", không thể ký.");
        }

        // Khởi tạo IP + timestamp
        Instant signedAt = Instant.now();

        // Render lần 1 → tính SHA-256
        byte[] pdfBytes = contractPdfGenerator.generateSignedPdfBytes(contract, ipAddress, signedAt);

        // lưu file
        String fileUrl;
        try {
            fileUrl = fileStorageService.storeContract(contract.getContractNumber(), pdfBytes);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu file hợp đồng: " + e.getMessage(), e);
        }

        // Cập nhật DB
        Instant today = Instant.now();
        contract.setIpAddress(ipAddress);
        contract.setSignedAt(signedAt);
        contract.setContractFileUrl(fileUrl);
        contract.setEffectiveDate(today);
        contract.setFeePaymentDeadline(today.plus(35, ChronoUnit.DAYS));
        contract.setEndDate(today.plus(30, ChronoUnit.DAYS));
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
    public void exportContractPdf(Long contractId, HttpServletResponse response) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng " + contractId));

        String fileName = "Hop_Dong_" + contract.getContractNumber();

        // Nếu hợp đồng đã có file vật lý trong DB, tải trực tiếp file từ ổ đĩa
        String fileUrl = contract.getContractFileUrl();
        if (fileUrl != null && !fileUrl.isBlank()) {
            String relativePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            java.io.File file = new java.io.File(relativePath);
            if (file.exists() && file.isFile()) {
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".pdf\"");
                try {
                    java.nio.file.Files.copy(file.toPath(), response.getOutputStream());
                    response.getOutputStream().flush();
                    return;
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi đọc file hợp đồng từ ổ đĩa: " + e.getMessage(), e);
                }
            }
        }

        String ip = contract.getIpAddress() != null ? contract.getIpAddress() : "N/A";
        Instant signed = contract.getSignedAt() != null ? contract.getSignedAt() : Instant.now();
        String hash = (contract.getStatus() == ContractStatus.ACTIVE || contract.getStatus() == ContractStatus.COMPLETED)
                ? "BẢN SAO TRÍCH XUẤT TỪ HỆ THỐNG"
                : "BẢN NHÁP - CHƯA KÝ CHÍNH THỨC";

        // Gói data và đẩy cho Template Method xuất ra trình duyệt
        ContractPdfPayload payload = new ContractPdfPayload(contract, ip, signed, hash);
        contractPdfGenerator.exportToHttpResponse(payload, response, fileName);
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

        // đồng bộ bảng transactions
        Optional<Transaction> pendingTxn = transactionRepository
                .findTopByContractIdAndStatusOrderByCreatedAtDesc(contractId, TransactionStatus.PENDING);

        if (pendingTxn.isPresent()) {
            // Đã có PENDING (Do gia sư từng bấm VNPay nhưng không trả tiền) -> Sửa nó thành SUCCESS
            Transaction txn = pendingTxn.get();
            txn.setStatus(TransactionStatus.SUCCESS);
            txn.setPaymentMethod(PaymentMethod.BANK_TRANSFER); // Ghi nhận là chuyển khoản thủ công
            txn.setPaidAt(Instant.now());
            txn.setNote("Admin (ID: " + adminId + ") xác nhận thu tiền thủ công");
            transactionRepository.save(txn);
        } else {
            // Chưa có giao dịch nào -> Tự động sinh mới 1 giao dịch SUCCESS
            Transaction newTxn = Transaction.builder()
                    .contract(contract)
                    .user(contract.getTutor().getUser())
                    .amount(contract.getIntroductionFee())
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .status(TransactionStatus.SUCCESS)
                    .transactionCode("MANUAL-HD" + contract.getId() + "-" + System.currentTimeMillis())
                    .paidAt(Instant.now())
                    .note("Admin (ID: " + adminId + ") tạo mới và xác nhận thu tiền")
                    .build();
            transactionRepository.save(newTxn);
        }

        // Cập nhật trạng thái dòng tiền
        contract.setIsFeePaid(true);
        contract.setPaidAt(Instant.now());

        // Nếu hợp đồng đã được gia sư ký trước đó rồi, tự động kích hoạt trạng thái ACTIVE luôn
        if (contract.getStatus() == ContractStatus.PENDING_SIGNATURE && contract.getSignedAt() != null) {
            contract.setStatus(ContractStatus.ACTIVE);
        }

        contractRepository.save(contract);
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

    @Override
    @Transactional
    public void completeContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Hợp đồng không tồn tại"));

        if (contract.getStatus() == ContractStatus.COMPLETED) {
            return;
        }

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException("Chỉ có thể hoàn thành hợp đồng đang ở trạng thái ACTIVE");
        }

        contract.setStatus(ContractStatus.COMPLETED);
        contractRepository.save(contract);

        User studentUser = contract.getClassRequest().getUser();

        Long studentUserId = studentUser != null ? studentUser.getId() : null;
        String studentEmail = studentUser != null ? studentUser.getEmail() : contract.getClassRequest().getContactEmail();
        String studentName = studentUser != null ? studentUser.getFullName() : contract.getClassRequest().getContactName();
        String tutorName = contract.getTutor().getUser().getFullName();

        eventPublisher.publishEvent(new ContractCompletedEvent(
                contract.getId(),
                contract.getContractNumber(),
                studentUserId,
                studentEmail,
                studentName,
                tutorName
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public void exportContractPdfForUser(Long contractId, Long userId, HttpServletResponse response) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng " + contractId));

        boolean isTutor = contract.getTutor() != null && contract.getTutor().getUser() != null &&
                contract.getTutor().getUser().getId().equals(userId);
        boolean isStudent = contract.getClassRequest() != null && contract.getClassRequest().getUser() != null &&
                contract.getClassRequest().getUser().getId().equals(userId);

        if (!isTutor && !isStudent) {
            throw BusinessException.forbidden("Bạn không có quyền tải xuống hợp đồng này.");
        }

        exportContractPdf(contractId, response);
    }
}

