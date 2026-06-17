package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.ApplicationRequest;
import com.tutornet.tutor_net.dto.response.ClassApplicationResponse;
import com.tutornet.tutor_net.entity.*;
import com.tutornet.tutor_net.enums.ApplicationStatus;
import com.tutornet.tutor_net.enums.ClassRequestStatus;
import com.tutornet.tutor_net.enums.ContractStatus;
import com.tutornet.tutor_net.event.TutorApplicationAcceptedEvent;
import com.tutornet.tutor_net.event.TutorAppliedEvent;
import com.tutornet.tutor_net.event.TutorRespondedToInviteEvent;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.mapper.ClassApplicationMapper;
import com.tutornet.tutor_net.repository.ClassApplicationRepository;
import com.tutornet.tutor_net.repository.ClassRequestRepository;
import com.tutornet.tutor_net.repository.ContractRepository;
import com.tutornet.tutor_net.repository.TutorProfileRepository;
import com.tutornet.tutor_net.service.ClassApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassApplicationServiceImpl implements ClassApplicationService {

    private final ClassApplicationRepository applicationRepo;
    private final ClassRequestRepository classRequestRepo;
    private final TutorProfileRepository tutorProfileRepo;
    private final ContractRepository contractRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final ClassApplicationMapper mapper;

    @Override
    public ClassApplicationResponse respondToDirectInvite(Long requestId, boolean isAccepted, String message, Long tutorUserId) {

        // Lấy hồ sơ Gia sư đang thao tác
        TutorProfile tutorProfile = tutorProfileRepo.findByUserId(tutorUserId)
                .orElseThrow(() -> new BusinessException("Bạn chưa có hồ sơ gia sư hoặc hồ sơ chưa được duyệt"));

        // Kiểm tra Yêu cầu lớp học có tồn tại không
        ClassRequest classRequest = classRequestRepo.findById(requestId)
                .orElseThrow(() -> ResourceNotFoundException.of("Yêu cầu lớp học", requestId));

        // Nghiệp vụ Validate bảo mật:
        // - Lớp này có phải gửi đích danh cho gia sư này không?
        // - Lớp còn ở trạng thái PENDING không (tránh việc lớp đã bị huỷ mà gia sư vẫn bấm nhận)?
        if (classRequest.getTargetTutor() == null || !classRequest.getTargetTutor().getId().equals(tutorProfile.getId())) {
            throw BusinessException.forbidden("Bạn không có quyền phản hồi lời mời này");
        }
        if (classRequest.getStatus() != ClassRequestStatus.PENDING) {
            throw BusinessException.validationFailed("Yêu cầu lớp học này đã được xử lý hoặc bị huỷ");
        }
        if (applicationRepo.existsByClassRequestIdAndTutorId(requestId, tutorProfile.getId())) {
            throw BusinessException.validationFailed("Bạn đã phản hồi lời mời này rồi");
        }

        // Tạo bản ghi Ứng tuyển / Phản hồi
        ApplicationStatus appStatus = isAccepted ? ApplicationStatus.ACCEPTED : ApplicationStatus.REJECTED;

        ClassApplication application = ClassApplication.builder()
                .classRequest(classRequest)
                .tutor(tutorProfile)
                .status(appStatus)
                .message(message)
                .build();

        ClassApplication savedApplication = applicationRepo.save(application);

        // Cập nhật lại trạng thái của Lớp học (ClassRequest)
        if (isAccepted) {
            classRequest.setStatus(ClassRequestStatus.MATCHED); // Chốt lớp thành công
        } else {
            classRequest.setStatus(ClassRequestStatus.CANCELLED); // Lớp bị huỷ do gia sư từ chối
        }
        classRequestRepo.save(classRequest);

        // Bắn Event để gửi thông báo/Email cho Học viên
        eventPublisher.publishEvent(new TutorRespondedToInviteEvent(
                classRequest.getId(),
                classRequest.getContactName(),
                classRequest.getContactEmail(),
                classRequest.getUser(),
                tutorProfile.getUser().getFullName(),
                isAccepted
        ));

        // Map Response (Có thể đưa vào Mapper Component)
        return mapper.toResponse(savedApplication);
    }

    @Override
    public ClassApplicationResponse applyForClass(Long classRequestId, ApplicationRequest.ApplyClassRequest request, Long tutorUserId) {
        TutorProfile tutor = tutorProfileRepo.findByUserId(tutorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ Gia sư của bạn"));

        ClassRequest classRequest = classRequestRepo.findById(classRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học không tồn tại"));

        if (classRequest.getUser() != null && classRequest.getUser().getId().equals(tutorUserId)) {
            throw new BusinessException("Bạn không thể ứng tuyển vào lớp học do chính mình tạo ra");
        }

        if (classRequest.getStatus() != ClassRequestStatus.APPROVED) {
            throw new BusinessException("Lớp học này hiện không nhận thêm ứng viên");
        }

        // Kiểm tra xem đã ứng tuyển trước đó chưa
        if (applicationRepo.existsByClassRequestIdAndTutorId(classRequestId, tutor.getId())) {
            throw new BusinessException("Bạn đã ứng tuyển vào lớp học này rồi.");
        }

        ClassApplication application = ClassApplication.builder()
                .classRequest(classRequest)
                .tutor(tutor)
                .message(request != null ? request.message() : null)
                .status(ApplicationStatus.PENDING)
                .build();

        application = applicationRepo.save(application);

        eventPublisher.publishEvent(new TutorAppliedEvent(
                application.getId(),
                classRequest.getContactName(),
                classRequest.getContactEmail(),
                classRequest.getUser(),
                tutor.getUser().getFullName()
        ));

        return mapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassApplicationResponse> getApplicationsForClass(Long classRequestId, Long studentUserId) {

        ClassRequest classRequest = classRequestRepo.findById(classRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học không tồn tại."));

        // Kiểm tra người gọi API có phải là chủ sở hữu của lớp học này không
        if (classRequest.getUser() == null || !classRequest.getUser().getId().equals(studentUserId)) {
            throw BusinessException.forbidden("Bạn không có quyền xem danh sách ứng viên của lớp học này.");
        }

        List<ClassApplication> applications = applicationRepo.findByClassRequestIdOrderByCreatedAtDesc(classRequestId);
        int applicantsCount = applicationRepo.countByClassRequestId(classRequest.getId());

        return applications.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Học viên chốt gia sư (Phê duyệt đơn ứng tuyển)
     */
    @Override
    public ClassApplicationResponse acceptApplication(Long classRequestId, Long applicationId, Long studentUserId) {

        // 1. Kiểm tra Lớp học và Quyền sở hữu
        ClassRequest classRequest = classRequestRepo.findById(classRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học không tồn tại."));

        if (classRequest.getUser() == null || !classRequest.getUser().getId().equals(studentUserId)) {
            throw BusinessException.forbidden("Bạn không có quyền thao tác trên lớp học này.");
        }

        if (classRequest.getStatus() != ClassRequestStatus.APPROVED) {
            throw BusinessException.validationFailed("Lớp học này không ở trạng thái chờ duyệt ứng viên.");
        }

        // 2. Kiểm tra Đơn ứng tuyển
        ClassApplication selectedApplication = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn ứng tuyển không tồn tại."));

        if (!selectedApplication.getClassRequest().getId().equals(classRequestId)) {
            throw BusinessException.validationFailed("Đơn ứng tuyển không thuộc về lớp học này.");
        }

        if (selectedApplication.getStatus() != ApplicationStatus.PENDING) {
            throw BusinessException.validationFailed("Đơn ứng tuyển này đã được xử lý.");
        }

        // 3. Cập nhật Đơn được chọn -> ACCEPTED
        selectedApplication.setStatus(ApplicationStatus.ACCEPTED);
        applicationRepo.save(selectedApplication);

        // 4. Cập nhật Lớp học -> MATCHED & Khóa lại (gắn Target Tutor)
        classRequest.setStatus(ClassRequestStatus.MATCHED);
        classRequest.setTargetTutor(selectedApplication.getTutor());
        classRequestRepo.save(classRequest);

        // 5. Từ chối các ứng viên còn lại (Auto-reject)
        List<ClassApplication> otherApplications = applicationRepo.findByClassRequestIdAndStatus(classRequestId, ApplicationStatus.PENDING)
                .stream()
                .filter(app -> !app.getId().equals(applicationId)) // Loại trừ người vừa được nhận
                .collect(Collectors.toList());

        for (ClassApplication app : otherApplications) {
            app.setStatus(ApplicationStatus.REJECTED);
        }
        if (!otherApplications.isEmpty()) {
            applicationRepo.saveAll(otherApplications);
            // Gợi ý: Có thể bắn Event gửi mail "Rất tiếc" cho danh sách otherApplications ở đây
        }

        // 6. GIAI ĐOẠN 3 (BƯỚC ĐỆM): Tự động tạo Hợp đồng chờ ký số
        BigDecimal hourlyRate = classRequest.getHourlyRate() != null ? classRequest.getHourlyRate() : classRequest.getProposedPrice();
        BigDecimal introFee = hourlyRate.multiply(BigDecimal.valueOf(16)).multiply(BigDecimal.valueOf(0.40));
        String contractNumber = "HD-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Contract contract = Contract.builder()
                .contractNumber(contractNumber)
                .classRequest(classRequest)
                .tutor(selectedApplication.getTutor())
                .introductionFee(introFee)
                .status(ContractStatus.PENDING_SIGNATURE)
                .freeTrialCount(1)
                .isFeePaid(false)
                .effectiveDate(LocalDate.now())
                .build();
        contractRepo.save(contract);

        // 7. Bắn Event báo tin cho Gia sư để họ vào ký hợp đồng
        eventPublisher.publishEvent(new TutorApplicationAcceptedEvent(
                classRequest.getId(),
                selectedApplication.getId(),
                selectedApplication.getTutor().getUser().getId(),
                selectedApplication.getTutor().getUser().getEmail(),
                selectedApplication.getTutor().getUser().getFullName(),
                classRequest.getContactName()
        ));

        return mapper.toResponse(selectedApplication);
    }
}
