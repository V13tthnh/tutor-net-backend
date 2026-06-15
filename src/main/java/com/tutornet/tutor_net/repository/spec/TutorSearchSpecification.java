package com.tutornet.tutor_net.repository.spec;

import com.tutornet.tutor_net.entity.*;
import com.tutornet.tutor_net.enums.GenderType;
import com.tutornet.tutor_net.enums.TeachingMode;
import com.tutornet.tutor_net.enums.TutorStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class TutorSearchSpecification {

    /** Chỉ lấy hồ sơ đã được duyệt */
    public static Specification<TutorProfile> isApproved() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), TutorStatus.APPROVED);
    }

    /** Tìm theo tên gia sư */
    public static Specification<TutorProfile> hasNameKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            Join<TutorProfile, User> user = root.join("user", JoinType.LEFT);
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.like(cb.lower(user.get("fullName")), pattern);
        };
    }

    /** Lọc theo danh sách môn học */
    public static Specification<TutorProfile> hasAnySubjectId(List<Long> subjectIds) {
        return (root, query, cb) -> {
            if (subjectIds == null || subjectIds.isEmpty()) return null;
            Subquery<Long> sub = query.subquery(Long.class);
            Root<TutorSubject> ts = sub.from(TutorSubject.class);
            sub.select(cb.literal(1L))
                    .where(
                            cb.equal(ts.get("tutor"), root),
                            ts.get("subject").get("id").in(subjectIds)
                    );
            return cb.exists(sub);
        };
    }

    /** Lọc theo tỉnh/thành phố dạy */
    public static Specification<TutorProfile> hasAnyProvince(List<String> provinces) {
        return (root, query, cb) -> {
            if (provinces == null || provinces.isEmpty()) return null;
            Subquery<Long> sub = query.subquery(Long.class);
            Root<TutorTeachingArea> ta = sub.from(TutorTeachingArea.class);
            sub.select(cb.literal(1L))
                    .where(
                            cb.equal(ta.get("tutor"), root),
                            ta.get("province").in(provinces)
                    );
            return cb.exists(sub);
        };
    }

    /** Lọc theo giới tính */
    public static Specification<TutorProfile> hasAnyGender(List<GenderType> genders) {
        return (root, query, cb) -> {
            if (genders == null || genders.isEmpty()) return null;
            Join<TutorProfile, User> user = root.join("user", JoinType.LEFT);
            return user.get("gender").in(genders);
        };
    }

    /** Lọc theo hình thức dạy — dùng native SQL vì teachingModes là Postgres array */
    public static Specification<TutorProfile> hasAnyTeachingMode(List<TeachingMode> modes) {
        return (root, query, cb) -> {
            if (modes == null || modes.isEmpty()) return null;
            return root.get("teachingMode").in(modes);
        };
    }
}
