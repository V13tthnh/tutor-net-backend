package com.tutornet.tutor_net.repository.spec;

import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.TutorInvitation;
import com.tutornet.tutor_net.entity.TutorProfile;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.enums.InvitationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TutorInvitationSpecification {

    public static Specification<TutorInvitation> filterForAdmin(
            String keyword, InvitationStatus status, Instant startDate, Instant endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Lọc theo keyword (Tìm trong Mã lớp, Tên Phụ huynh, Tên Gia sư)
            if (keyword != null && !keyword.isBlank()) {
                String likeKeyword = "%" + keyword.toLowerCase().trim() + "%";

                // Nối bảng (Join)
                Join<TutorInvitation, ClassRequest> classJoin = root.join("classRequest", JoinType.LEFT);
                Join<TutorInvitation, TutorProfile> tutorJoin = root.join("tutor", JoinType.LEFT);
                Join<TutorProfile, User> userJoin = tutorJoin.join("user", JoinType.LEFT);

                Predicate matchClassCode = cb.like(cb.lower(classJoin.get("classCode")), likeKeyword);
                Predicate matchStudentName = cb.like(cb.lower(classJoin.get("contactName")), likeKeyword);
                Predicate matchTutorName = cb.like(cb.lower(userJoin.get("fullName")), likeKeyword);

                predicates.add(cb.or(matchClassCode, matchStudentName, matchTutorName));
            }

            // Lọc theo trạng thái
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Lọc theo khoảng thời gian
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
