package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.ReviewCreateRequest;
import com.tutornet.tutor_net.dto.response.AdminReviewResponse;
import com.tutornet.tutor_net.dto.response.ContractResponse;
import com.tutornet.tutor_net.dto.response.PublicReviewResponse;
import com.tutornet.tutor_net.entity.Contract;
import com.tutornet.tutor_net.entity.Review;
import com.tutornet.tutor_net.entity.TutorProfile;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.enums.ContractStatus;
import com.tutornet.tutor_net.event.NewReviewSubmittedEvent;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.mapper.ContractMapper;
import com.tutornet.tutor_net.mapper.ReviewMapper;
import com.tutornet.tutor_net.repository.ContractRepository;
import com.tutornet.tutor_net.repository.ReviewRepository;
import com.tutornet.tutor_net.repository.TutorProfileRepository;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ContractRepository contractRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final ReviewMapper reviewMapper;
    private final ContractMapper contractMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<PublicReviewResponse> getPublicReviewsByTutor(Long tutorId, int page, int size) {
        // Sắp xếp các đánh giá mới nhất lên đầu tiên
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());

        Page<Review> reviewsPage = reviewRepository.findByTutorIdAndIsPublicTrue(tutorId, pageable);

        return reviewsPage.map(reviewMapper::toPublicResponse);
    }

    @Override
    @Transactional
    public void submitReview(ReviewCreateRequest request, CustomUserDetails currentUser) {
        // Kiểm tra Hợp đồng
        Contract contract = contractRepository.findById(request.contractId())
                .orElseThrow(() -> new ResourceNotFoundException("Hợp đồng không tồn tại"));

        if (contract.getStatus() != ContractStatus.COMPLETED) {
            throw new BusinessException("Lớp học chưa hoàn thành, không thể đánh giá lúc này");
        }

        if (reviewRepository.existsByContractId(contract.getId())) {
            throw new BusinessException("Hợp đồng này đã được đánh giá");
        }

        // Phân luồng Xác thực (Login vs Magic Link)
        User reviewer = null;
        String tokenToSave = null;

        if (request.guestReviewToken() != null && !request.guestReviewToken().isEmpty()) {
            // Luồng 2: Khách vãng lai bấm từ EMAIL (hoặc người dùng đang đăng nhập sử dụng Magic Link)
            // Verify token có thuộc đúng contract không
            if (contract.getGuestReviewToken() == null ||
                    !contract.getGuestReviewToken().equals(request.guestReviewToken())) {
                throw new BusinessException("Đường dẫn đánh giá không hợp lệ hoặc đã hết hạn.");
            }

            // Check đã dùng chưa (token đã lưu vào reviews)
            if (reviewRepository.existsByGuestReviewToken(request.guestReviewToken())) {
                throw new BusinessException("Đường dẫn đánh giá này đã được sử dụng");
            }

            tokenToSave = request.guestReviewToken();

            // Nếu người dùng hiện tại đang đăng nhập trùng khớp với người thuê lớp, vẫn ghi nhận tác giả đánh giá
            if (currentUser != null && contract.getClassRequest().getUser() != null &&
                    contract.getClassRequest().getUser().getId().equals(currentUser.getUser().getId())) {
                reviewer = currentUser.getUser();
            }
        } else {
            // Luồng 1: Người dùng ĐÃ ĐĂNG NHẬP trên hệ thống (không có token trong link)
            if (currentUser == null) {
                throw new BusinessException("Bạn cần đăng nhập để thực hiện đánh giá");
            }
            if (contract.getClassRequest().getUser() == null ||
                    !contract.getClassRequest().getUser().getId().equals(currentUser.getUser().getId())) {
                throw new BusinessException("Bạn không có quyền đánh giá lớp học này");
            }
            reviewer = currentUser.getUser();
        }

        // 3. Tạo Review và lưu vào DB (Theo đúng Entity của bạn)
        Review newReview = reviewMapper.toEntity(request, contract, reviewer, tokenToSave);
        reviewRepository.save(newReview);

        // 4. Tính toán và cập nhật điểm cho Gia sư
        syncTutorRating(contract.getTutor().getUser().getId());

        String reviewerName = reviewer != null ? reviewer.getFullName() : "Học viên ẩn danh";

        eventPublisher.publishEvent(new NewReviewSubmittedEvent(
                contract.getTutor().getUser().getId(),
                contract.getTutor().getUser().getEmail(),
                reviewerName,
                newReview.getRating(),
                contract.getId()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> getReviewsForAdmin(Integer rating, Boolean isPublic, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // 🌟 Xử lý cờ boolean và chống lỗi null an toàn
        boolean hasSearch = (search != null && !search.trim().isEmpty());
        String safeSearch = hasSearch ? search.trim() : "";

        // Cập nhật lại việc gọi hàm với tham số safeSearch và hasSearch
        Page<Review> reviewsPage = reviewRepository.findAllForAdmin(rating, isPublic, safeSearch, hasSearch, pageable);

        return reviewsPage.map(reviewMapper::toAdminResponse);
    }

    @Override
    @Transactional
    public void toggleReviewVisibility(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá có ID: " + reviewId));

        // Đảo ngược trạng thái hiển thị (Đang hiện thành ẩn, đang ẩn thành hiện)
        review.setIsPublic(!review.getIsPublic());
        reviewRepository.save(review);

        // Sau khi ẩn/hiện, tính toán lại điểm trung bình cho gia sư ngay lập tức
        syncTutorRating(review.getTutor().getUser().getId());
    }

    /**
     * Hàm dùng chung để Đồng bộ tính toán lại Điểm và Lượt đánh giá
     */
    private void syncTutorRating(Long tutorUserId) {
        Double avg = reviewRepository.calculateAverageRatingByTutorId(tutorUserId);
        Integer count = reviewRepository.countPublicReviewsByTutorId(tutorUserId);

        // Xử lý trường hợp null (khi gia sư bị ẩn hết tất cả các đánh giá)
        BigDecimal newAvg = (avg != null)
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        int newCount = (count != null) ? count : 0;

        // Tìm Profile của gia sư và cập nhật
        TutorProfile profile = tutorProfileRepository.findByUserId(tutorUserId)
                .orElse(null); // Tránh quăng lỗi nếu vì lý do nào đó profile bị thiếu

        if (profile != null) {
            profile.setRatingAvg(newAvg);
            profile.setRatingCount(newCount);
            tutorProfileRepository.save(profile);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getGuestContract(Long contractId, String token) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Hợp đồng không tồn tại"));

        if (contract.getGuestReviewToken() == null || !contract.getGuestReviewToken().equals(token)) {
            throw new BusinessException("Đường dẫn đánh giá không hợp lệ hoặc đã hết hạn.");
        }

        if (reviewRepository.existsByGuestReviewToken(token)) {
            throw new BusinessException("Đường dẫn đánh giá này đã được sử dụng");
        }

        if (contract.getStatus() != ContractStatus.COMPLETED) {
            throw new BusinessException("Lớp học chưa hoàn thành, không thể đánh giá lúc này");
        }

        return contractMapper.toResponse(contract);
    }
}
