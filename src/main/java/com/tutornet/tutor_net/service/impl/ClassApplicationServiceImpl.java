package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.ApplicationRequest;
import com.tutornet.tutor_net.dto.response.ClassApplicationResponse;
import com.tutornet.tutor_net.entity.*;
import com.tutornet.tutor_net.enums.ApplicationStatus;
import com.tutornet.tutor_net.enums.ClassRequestStatus;
import com.tutornet.tutor_net.event.TutorAppliedEvent;
import com.tutornet.tutor_net.event.TutorRespondedToInviteEvent;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.mapper.ClassApplicationMapper;
import com.tutornet.tutor_net.repository.ClassApplicationRepository;
import com.tutornet.tutor_net.repository.ClassRequestRepository;
import com.tutornet.tutor_net.repository.TutorProfileRepository;
import com.tutornet.tutor_net.service.ClassApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassApplicationServiceImpl implements ClassApplicationService {

    private final ClassApplicationRepository applicationRepo;
    private final ClassRequestRepository requestRepo;
    private final TutorProfileRepository tutorProfileRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final ClassApplicationMapper mapper;

    @Override
    public ClassApplicationResponse respondToDirectInvite(Long requestId, boolean isAccepted, String message, Long tutorUserId) {

        // 1. Lấy hồ sơ Gia sư đang thao tác
        TutorProfile tutorProfile = tutorProfileRepo.findByUserId(tutorUserId)
                .orElseThrow(() -> new BusinessException("Bạn chưa có hồ sơ gia sư hoặc hồ sơ chưa được duyệt"));

        // 2. Kiểm tra Yêu cầu lớp học có tồn tại không
        ClassRequest classRequest = requestRepo.findById(requestId)
                .orElseThrow(() -> ResourceNotFoundException.of("Yêu cầu lớp học", requestId));

        // 3. Nghiệp vụ Validate bảo mật:
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

        // 4. Tạo bản ghi Ứng tuyển / Phản hồi
        ApplicationStatus appStatus = isAccepted ? ApplicationStatus.ACCEPTED : ApplicationStatus.REJECTED;

        ClassApplication application = ClassApplication.builder()
                .classRequest(classRequest)
                .tutor(tutorProfile)
                .status(appStatus)
                .message(message)
                .build();

        ClassApplication savedApplication = applicationRepo.save(application);

        // 5. Cập nhật lại trạng thái của Lớp học (ClassRequest)
        if (isAccepted) {
            classRequest.setStatus(ClassRequestStatus.MATCHED); // Chốt lớp thành công
        } else {
            classRequest.setStatus(ClassRequestStatus.CANCELLED); // Lớp bị huỷ do gia sư từ chối
        }
        requestRepo.save(classRequest);

        // 6. Bắn Event để gửi thông báo/Email cho Học viên
        eventPublisher.publishEvent(new TutorRespondedToInviteEvent(
                classRequest.getId(),
                classRequest.getContactName(),
                classRequest.getContactEmail(),
                classRequest.getUser(),
                tutorProfile.getUser().getFullName(),
                isAccepted
        ));

        // 7. Map Response (Có thể đưa vào Mapper Component)
        return new ClassApplicationResponse(
                savedApplication.getId(),
                classRequest.getId(),
                tutorProfile.getId(),
                tutorProfile.getUser().getFullName(),
                tutorProfile.getUser().getAvatarUrl(),
                tutorProfile.getUniversity(),
                tutorProfile.getMajor(),
                savedApplication.getStatus(),
                savedApplication.getMessage(),
                savedApplication.getCreatedAt()
        );
    }

    @Override
    public ClassApplicationResponse applyForClass(ApplicationRequest.ApplyClassRequest request, Long tutorUserId) {
        // 1. Lấy hồ sơ gia sư từ userId
        TutorProfile tutor = tutorProfileRepo.findByUserId(tutorUserId)
                .orElseThrow(() -> new BusinessException("Gia sư chưa hoàn thiện hồ sơ"));

        // 2. Lấy thông tin lớp học
        ClassRequest classReq = requestRepo.findById(request.requestId())
                .orElseThrow(() -> ResourceNotFoundException.of("Lớp học", request.requestId()));

        // 3. Kiểm tra tính hợp lệ
        if (applicationRepo.existsByClassRequestIdAndTutorId(classReq.getId(), tutor.getId())) {
            throw new BusinessException("Bạn đã gửi yêu cầu ứng tuyển lớp này rồi");
        }

        // 4. Lưu bản ghi ứng tuyển
        ClassApplication app = ClassApplication.builder()
                .classRequest(classReq)
                .tutor(tutor)
                .status(ApplicationStatus.PENDING)
                .message(request.message())
                .build();

        ClassApplication savedApp = applicationRepo.save(app);

        // 5. Kích hoạt Event để thông báo cho Học viên (bất kể vãng lai hay đăng nhập)
        eventPublisher.publishEvent(new TutorAppliedEvent(
                classReq.getId(),
                classReq.getContactName(),
                classReq.getContactEmail(), // Dùng để gửi email nếu là vãng lai
                classReq.getUser(),        // Dùng để gửi Notification nếu đã đăng nhập
                tutor.getUser().getFullName()
        ));

        return mapper.toResponse(savedApp);
    }
}
