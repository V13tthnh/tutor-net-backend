package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.ClassRequest.CreateClassRequest;
import com.tutornet.tutor_net.dto.request.ClassRequest.ReviewClassRequest;
import com.tutornet.tutor_net.dto.request.ClassRequest.BulkReviewClassRequest;
import com.tutornet.tutor_net.dto.response.ClassRequestDropdownResponse;
import com.tutornet.tutor_net.dto.response.ClassRequestFilterOptionsResponse;
import com.tutornet.tutor_net.dto.request.ClassRequest.TrackClassRequest;
import com.tutornet.tutor_net.dto.response.ClassRequestResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.entity.*;
import com.tutornet.tutor_net.enums.ClassRequestStatus;
import com.tutornet.tutor_net.enums.InvitationStatus;
import com.tutornet.tutor_net.enums.TeachingMode;
import com.tutornet.tutor_net.event.ClassRequestNotificationEvent;
import com.tutornet.tutor_net.event.ClassRequestReviewedEvent;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.exception.TooManyRequestsException;
import com.tutornet.tutor_net.mapper.ClassRequestMapper;
import com.tutornet.tutor_net.repository.*;
import com.tutornet.tutor_net.service.ClassRequestService;
import com.tutornet.tutor_net.service.MailService;
import com.tutornet.tutor_net.service.RateLimiterService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassRequestServiceImpl implements ClassRequestService {

    private final ClassRequestRepository classRequestRepo;
    private final ClassApplicationRepository applicationRepo;
    private final SubjectRepository subjectRepo;
    private final TutorProfileRepository tutorProfileRepo;
    private final UserRepository userRepository;
    private final TutorInvitationRepository tutorInvitationRepository;
    private final ClassRequestMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final MailService mailService;
    private final RateLimiterService rateLimiterService;

    @Override
    @Transactional(readOnly = true)
    public UserRoleResponse.PageResponse<ClassRequestResponse> getJobBoardRequests(
            Long tutorUserId, Long subjectId, String teachingModeStr, Pageable pageable) {

        // 1. Lấy ID hồ sơ gia sư (Giữ nguyên)
        Long tutorProfileId = null;
        if (tutorUserId != null) {
            tutorProfileId = tutorProfileRepo.findByUserId(tutorUserId)
                    .map(TutorProfile::getId)
                    .orElse(null);
        }

        // 2. Parse & validate teachingMode (Giữ nguyên)
        String teachingModeName = null;
        if (teachingModeStr != null && !teachingModeStr.isBlank()) {
            try {
                teachingModeName = TeachingMode.valueOf(teachingModeStr.toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw BusinessException.validationFailed(
                        "Hình thức học không hợp lệ. Vui lòng chọn ONLINE, OFFLINE hoặc HYBRID.");
            }
        }

        // CHÍNH SỬA TẠI ĐÂY: Chuyển đổi pageable sang dạng Native SQL trước khi gọi Repo
        Pageable nativePageable = convertToNativePageable(pageable);

        // 3. Query — Thay thế pageable cũ bằng nativePageable
        Page<ClassRequest> requestPage = classRequestRepo.findAvailableRequestsForJobBoard(
                tutorProfileId,
                subjectId,
                teachingModeName,
                nativePageable // <--- Đổi ở đây
        );

        // 4. Map sang DTO & 5. Đóng gói PageResponse (Giữ nguyên đoạn code phía dưới của bạn)
        List<ClassRequestResponse> content = requestPage.getContent().stream().map(classRequest -> {
            int applicantsCount = applicationRepo.countByClassRequestId(classRequest.getId());
            return mapper.toResponseWithCount(classRequest, applicantsCount);
        }).collect(Collectors.toList());

        return new UserRoleResponse.PageResponse<>(
                content,
                requestPage.getNumber() + 1,
                requestPage.getSize(),
                requestPage.getTotalElements(),
                requestPage.getTotalPages(),
                requestPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ClassRequestResponse trackClassRequest(TrackClassRequest request, String clientIp) {
        String actionKey = "TRACKING_IP_" + clientIp;

        if (rateLimiterService.isBlocked(actionKey)) {
            throw new TooManyRequestsException("Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút");
        }

        String cleanClassCode = request.classCode() != null ? request.classCode().trim().toUpperCase() : "";
        String cleanPhone = request.contactPhone() != null ? request.contactPhone().trim().replace(" ", "") : "";

        ClassRequest classRequest = classRequestRepo
                .findByClassCodeAndContactPhone(cleanClassCode, cleanPhone)
                .orElse(null); // orElseGet để tự xử lý lỗi bên dưới

        // Nếu  Không tìm thấy
        if (classRequest == null) {
            // Ghi nhận 1 lần sai (Tối đa 5 lần, khóa 15 phút)
            rateLimiterService.recordFailedAttempt(actionKey, 5, 15);
            throw new ResourceNotFoundException("Không tìm thấy yêu cầu lớp học nào khớp với Mã lớp và Số điện thoại này");
        }

        // Nếu tìm thấy Reset bộ đếm an toàn
        rateLimiterService.resetAttempts(actionKey);

        boolean hasAccount = userRepository.existsByPhone(classRequest.getContactPhone());

        return mapper.toTrackingResponse(classRequest, hasAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRequestDropdownResponse> getMyActiveRequestsForDropdown(Long userId) {
        return classRequestRepo.findDropdownByUserIdAndStatuses(
                userId,
                List.of(ClassRequestStatus.PENDING, ClassRequestStatus.APPROVED)
        );
    }

    @Override
    @Transactional
    public ClassRequestResponse createClassRequest(CreateClassRequest request, Long authenticatedUserId) {

        // Nếu học OFFLINE thì bắt buộc phải nhập địa chỉ chi tiết
        TeachingMode mode = TeachingMode.valueOf(request.teachingMode().toUpperCase());
        if (mode == TeachingMode.OFFLINE && (request.addressDetail() == null || request.addressDetail().isBlank())) {
            throw BusinessException.validationFailed("Địa chỉ chi tiết là bắt buộc đối với hình thức học trực tiếp (OFFLINE)");
        }

        // Kiểm tra sự tồn tại của Môn học (Subject)
        Subject subject = subjectRepo.findById(request.subjectId())
                .orElseThrow(() -> ResourceNotFoundException.of("Môn học", request.subjectId()));

        // Khởi tạo đối tượng Entity
        ClassRequest classRequest = mapper.toEntity(request);
        classRequest.setSubject(subject);
        classRequest.setStatus(ClassRequestStatus.PENDING);

        // Liên kết tài khoản nếu Khách hàng đã đăng nhập hệ thống
        if (authenticatedUserId != null) {
            User user = userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Tài khoản", authenticatedUserId));
            classRequest.setUser(user);
        }

        // Xử lý logic khi chọn "Mời đích danh Gia sư" từ trang Profile
        TutorProfile targetTutor = null;
        if (request.targetTutorId() != null) {
            targetTutor = tutorProfileRepo.findById(request.targetTutorId())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy gia sư cần mời"));
            classRequest.setTargetTutor(targetTutor);
        }

        // thông báo trực tiếp cho Gia sư mục tiêu biết rằng họ có lời mời dạy mới.
        if (request.contactEmail() != null && !request.contactEmail().isBlank()) {
            mailService.sendClassRequestConfirmationEmail(request.contactEmail(), request.contactName(), subject.getName());
        }

        if (targetTutor != null) {
            mailService.sendTutorDirectInviteEmail(
                    targetTutor.getUser().getEmail(),
                    targetTutor.getUser().getFullName(),
                    subject.getName());

        }

        // TÍNH LƯƠNG GIỜ ĐỒNG BỘ
        // Công thức: 1 tháng = 4 tuần
        // Lương giờ = Tổng lương / (Số buổi/tuần * 4 * Số giờ/buổi)
        double hoursPerSession = request.durationMinutes() / 60.0;
        // Tổng số giờ trong 1 tháng (4 tuần)
        BigDecimal totalHoursPerMonth = BigDecimal.valueOf(request.sessionsPerWeek() * 4 * hoursPerSession);

        // Tính ra lương giờ
        BigDecimal hourlyRate = request.proposedPrice().divide(totalHoursPerMonth, 2, RoundingMode.HALF_UP);

        // Set vào entity trước khi save
        classRequest.setHourlyRate(hourlyRate);
        classRequest.setSessionsPerWeek(request.sessionsPerWeek());
        classRequest.setDurationMinutes(request.durationMinutes());

        // Lưu xuống Cơ sở dữ liệu
        ClassRequest savedRequest = classRequestRepo.save(classRequest);

        // BẮN EVENT ĐỂ XỬ LÝ GỬI MAIL VÀ THÔNG BÁO (Chạy ngầm sau khi Commit DB)
        // NẾU NGƯỜI DÙNG MUỐN MỜI ĐÍCH DANH GIA SƯ (Tạo Lời Mời)
        if (targetTutor != null) {
            // Tự động sinh bản ghi vào bảng tutor_invitations
            TutorInvitation invitation = TutorInvitation.builder()
                    .tutor(targetTutor)
                    .classRequest(savedRequest) // Liên kết khóa ngoại
                    .message(request.studentNotes()) // Mượn trường note làm message
                    .status(InvitationStatus.PENDING)
                    .build();
            tutorInvitationRepository.save(invitation);

            // Gửi Mail cho gia sư
            mailService.sendTutorDirectInviteEmail(
                    targetTutor.getUser().getEmail(),
                    targetTutor.getUser().getFullName(),
                    subject.getName()
            );
        }

        eventPublisher.publishEvent(new ClassRequestNotificationEvent(
                savedRequest.getId(),
                subject.getName(),
                targetTutor != null ? targetTutor.getUser() : null
        ));

        return mapper.toResponse(savedRequest);
    }


    @Override
    @Transactional
    public List<ClassRequestResponse> createBulkClassRequests(List<CreateClassRequest> requests, Long userId) {
        List<ClassRequestResponse> responses = new ArrayList<>();

        for (CreateClassRequest request : requests) {
            // Tái sử dụng lại logic của hàm createClassRequest hiện có
            responses.add(createClassRequest(request, userId));
        }

        return responses;
    }


    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Lấy danh sách class request (filter linh hoạt, không ràng buộc theo gia sư)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public UserRoleResponse.PageResponse<ClassRequestResponse> getAllRequestsForAdmin(
            String keyword, String statusStr, Long subjectId, String teachingModeStr, Pageable pageable) {

        // 1. Chuẩn hoá keyword & 2. Parse status filter & 3. Parse teachingMode (Giữ nguyên logic cũ của bạn)
        String normalizedKeyword = (keyword != null && !keyword.isBlank())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;

        ClassRequestStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try { status = ClassRequestStatus.valueOf(statusStr.toUpperCase()); }
            catch (IllegalArgumentException e) { throw BusinessException.validationFailed("Trạng thái không hợp lệ."); }
        }

        TeachingMode teachingMode = null;
        if (teachingModeStr != null && !teachingModeStr.isBlank()) {
            try { teachingMode = TeachingMode.valueOf(teachingModeStr.toUpperCase()); }
            catch (IllegalArgumentException e) { throw BusinessException.validationFailed("Hình thức học không hợp lệ."); }
        }

        // CHÍNH SỬA TẠI ĐÂY: Chuyển đổi pageable sang dạng Native SQL
        Pageable nativePageable = convertToNativePageable(pageable);

        // 4. Query — Thay thế pageable cũ bằng nativePageable
        Page<ClassRequest> requestPage = classRequestRepo.findAllForAdmin(
                normalizedKeyword,
                status != null ? status.name() : null,
                subjectId,
                teachingMode != null ? teachingMode.name() : null,
                nativePageable // <--- Đổi ở đây
        );

        // 5. Map sang DTO (Giữ nguyên)
        List<ClassRequestResponse> content = requestPage.getContent().stream()
                .map(cr -> {
                    int count = applicationRepo.countByClassRequestId(cr.getId());
                    return mapper.toResponseWithCount(cr, count);
                })
                .collect(Collectors.toList());

        return new UserRoleResponse.PageResponse<>(
                content,
                requestPage.getNumber() + 1,
                requestPage.getSize(),
                requestPage.getTotalElements(),
                requestPage.getTotalPages(),
                requestPage.isLast()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Xem chi tiết 1 class request
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ClassRequestResponse getRequestDetailForAdmin(Long id) {
        ClassRequest classRequest = classRequestRepo.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Yêu cầu tạo lớp", id));

        int count = applicationRepo.countByClassRequestId(classRequest.getId());
        return mapper.toResponseWithCount(classRequest, count);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Duyệt hoặc từ chối class request
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ClassRequestResponse reviewClassRequest(Long classRequestId,
                                                   ReviewClassRequest reviewRequest,
                                                   Long adminId) {

        // Kiểm tra class request tồn tại
        ClassRequest classRequest = classRequestRepo.findByIdWithUser(classRequestId)
                .orElseThrow(() -> ResourceNotFoundException.of("Yêu cầu tạo lớp", classRequestId));

        // Chỉ cho phép duyệt khi đang ở trạng thái PENDING
        if (classRequest.getStatus() != ClassRequestStatus.PENDING) {
            throw BusinessException.validationFailed(
                    "Chỉ có thể duyệt yêu cầu đang ở trạng thái PENDING. " +
                            "Trạng thái hiện tại: " + classRequest.getStatus());
        }

        // Validate nghiệp vụ: REJECTED thì bắt buộc phải có lý do
        ClassRequestStatus newStatus = reviewRequest.status();

        if (newStatus == ClassRequestStatus.REJECTED) {
            if (reviewRequest.rejectionReason() == null || reviewRequest.rejectionReason().isBlank()) {
                throw BusinessException.validationFailed("Vui lòng nhập lý do từ chối.");
            }
            // Chỉ APPROVED và REJECTED mới là hành động hợp lệ từ Admin
        } else if (newStatus != ClassRequestStatus.APPROVED) {
            throw BusinessException.validationFailed(
                    "Hành động không hợp lệ. Admin chỉ được phép APPROVED hoặc REJECTED.");
        }

        // Cập nhật trạng thái & lý do từ chối lên entity
        classRequest.setStatus(newStatus);
        classRequest.setRejectionReason(
                newStatus == ClassRequestStatus.REJECTED ? reviewRequest.rejectionReason() : null
        );

        ClassRequest savedRequest = classRequestRepo.save(classRequest);

        // 5. Gửi mail thông báo cho người đăng lớp
        //    Ưu tiên email liên hệ; nếu null (khách không nhập email) thì bỏ qua
        String recipientEmail = savedRequest.getContactEmail();
        String recipientName  = savedRequest.getContactName();
        String subjectName    = savedRequest.getSubject().getName();

        if (recipientEmail != null && !recipientEmail.isBlank()) {
            if (newStatus == ClassRequestStatus.APPROVED) {
                mailService.sendClassRequestApprovedEmail(
                        recipientEmail,
                        recipientName,
                        subjectName
                );
            } else {
                mailService.sendClassRequestRejectedEmail(
                        recipientEmail,
                        recipientName,
                        subjectName,
                        reviewRequest.rejectionReason()
                );
            }
        }

        if (savedRequest.getUser() != null) {
            eventPublisher.publishEvent(new ClassRequestReviewedEvent(
                    savedRequest.getUser(),
                    savedRequest.getId(),
                    subjectName,
                    newStatus == ClassRequestStatus.APPROVED,
                    reviewRequest.rejectionReason()
            ));
        }

        int count = applicationRepo.countByClassRequestId(savedRequest.getId());
        return mapper.toResponseWithCount(savedRequest, count);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassRequestFilterOptionsResponse getClassRequestFilterOptions() {

        // Lấy danh sách Môn học thực tế từ Database
        List<ClassRequestFilterOptionsResponse.SubjectOption> subjects = classRequestRepo.findDistinctSubjectsInRequests().stream()
                .map(s -> new ClassRequestFilterOptionsResponse.SubjectOption(s.getId(), s.getName()))
                .collect(Collectors.toList());

        // 2. Định nghĩa danh sách Trạng thái từ Enum ClassRequestStatus kèm Label tiếng Việt
        List<ClassRequestFilterOptionsResponse.StatusOption> statuses = Arrays.stream(ClassRequestStatus.values())
                .map(status -> {
                    String label = switch (status) {
                        case PENDING -> "Chờ duyệt";
                        case APPROVED -> "Đã duyệt (Công khai)";
                        case REJECTED -> "Từ chối duyệt";
                        case MATCHED -> "Đã chốt gia sư";
                        case CANCELLED -> "Đã hủy lớp";
                        // Thêm nhánh default này để sửa triệt để lỗi ép buộc bao phủ của Java compiler
                        default -> "Không xác định";
                    };
                    return new ClassRequestFilterOptionsResponse.StatusOption(status.name(), label);
                })
                .collect(Collectors.toList());

        // 3. Định nghĩa danh sách Hình thức học từ Enum TeachingMode kèm Label tiếng Việt
        List<ClassRequestFilterOptionsResponse.TeachingModeOption> teachingModes = Arrays.stream(TeachingMode.values())
                .map(mode -> {
                    String label = switch (mode) {
                        case ONLINE -> "Trực tuyến (Online)";
                        case OFFLINE -> "Tại nhà (Offline)";
                        case HYBRID -> "Linh hoạt (Hybrid)";
                        default -> "Hình thức khác"; // Thêm phòng hờ
                    };
                    return new ClassRequestFilterOptionsResponse.TeachingModeOption(mode.name(), label);
                })
                .collect(Collectors.toList());

        return new ClassRequestFilterOptionsResponse(statuses, subjects, teachingModes);
    }

    private Pageable convertToNativePageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "created_at"));
        }

        // Map các trường từ CamelCase (Java) sang SnakeCase (Database)
        List<Sort.Order> nativeOrders = pageable.getSort().stream()
                .map(order -> {
                    String property = order.getProperty();
                    switch (property) {
                        case "createdAt" -> property = "created_at";
                        case "updatedAt" -> property = "updated_at";
                        case "proposedPrice" -> property = "proposed_price";
                        case "contactName" -> property = "contact_name";
                        default -> property = "created_at";
                    }
                    return new Sort.Order(order.getDirection(), property);
                })
                .collect(Collectors.toList());

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(nativeOrders));
    }

    @Override
    @Transactional
    public List<ClassRequestResponse> reviewBulkClassRequests(BulkReviewClassRequest bulkRequest, Long adminId) {
        List<ClassRequestResponse> responses = new ArrayList<>();

        // Đóng gói thông tin trạng thái và lý do từ chối thành DTO đơn lẻ để tái sử dụng
        ReviewClassRequest singleReviewRequest = new ReviewClassRequest(
                bulkRequest.status(),
                bulkRequest.rejectionReason()
        );

        // Chạy vòng lặp xử lý từng ID trong danh sách được gửi lên
        for (Long id : bulkRequest.ids()) {
            // Gọi lại hàm xử lý đơn lẻ giúp kế thừa toàn bộ logic nghiệp vụ và cơ chế gửi email duyệt/từ chối
            ClassRequestResponse response = reviewClassRequest(id, singleReviewRequest, adminId);
            responses.add(response);
        }

        return responses;
    }
}


