package com.tutornet.tutor_net.visitor;

import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.TutorProfile;
import com.tutornet.tutor_net.enums.EduLevel;

import java.math.BigDecimal;

/**
 * Thuật toán tính phí nhận lớp nội bộ không cần truy xuất Database.
 * Thỏa mãn Design Pattern bằng cách code cứng các quy tắc kinh doanh dựa trên dữ liệu hiện có.
 */
public class StandardFeeCalculatorVisitor implements FeeVisitor {
    private final BigDecimal baseTuition;
    private double feePercentage = 0.40; // Phí cơ bản mặc định là 40%

    public StandardFeeCalculatorVisitor(BigDecimal baseTuition) {
        this.baseTuition = baseTuition;
    }

    @Override
    public void visit(TutorProfile tutor) {
        // Quy tắc 1: Sinh viên (Cao đẳng / THPT) thì thu phí rẻ hơn để hỗ trợ (giảm 5%)
        if (tutor.getEducationLevel() == EduLevel.ASSOCIATE || tutor.getEducationLevel() == EduLevel.HIGH_SCHOOL) {
            feePercentage -= 0.05;
        }
        // Quy tắc 2: Gia sư có đánh giá trên 4.5 sao & có kinh nghiệm -> Giảm thêm 5% phí để giữ chân
        if (tutor.getRatingAvg() != null && tutor.getRatingAvg().compareTo(new BigDecimal("4.5")) >= 0
                && tutor.getExperienceYears() >= 2) {
            feePercentage -= 0.05;
        }
    }

    @Override
    public void visit(ClassRequest classRequest) {
        // Quy tắc 3: Lớp cấp 3 hoặc Luyện thi Đại học khó tìm người dạy -> Hệ thống thu phí cao hơn 5%
        if (classRequest.getGradeLevel() != null &&
                (classRequest.getGradeLevel().contains("THPT") || classRequest.getGradeLevel().toUpperCase().contains("LUYỆN THI"))) {
            feePercentage += 0.05;
        }
    }

    /**
     * Trả về số tiền phí giao lớp cuối cùng
     */
    public BigDecimal getCalculatedFee() {
        // Đảm bảo mức phí không bao giờ bị âm hoặc rớt xuống dưới 20%
        if (feePercentage < 0.20) {
            feePercentage = 0.20;
        }
        return baseTuition.multiply(BigDecimal.valueOf(feePercentage));
    }
}
