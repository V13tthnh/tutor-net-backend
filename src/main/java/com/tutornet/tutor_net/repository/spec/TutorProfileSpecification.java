package com.tutornet.tutor_net.repository.spec;


import com.tutornet.tutor_net.entity.*;
import com.tutornet.tutor_net.enums.TutorStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class TutorProfileSpecification {

    public static Specification<TutorProfile> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            Join<TutorProfile, User> user = root.join("user", JoinType.LEFT);
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(user.get("fullName")), pattern),
                    cb.like(cb.lower(user.get("email")), pattern)
            );
        };
    }

    public static Specification<TutorProfile> hasAnyStatus(List<TutorStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }

            return root.get("status").in(statuses);
        };
    }

    public static Specification<TutorProfile> hasAnySubject(List<Long> subjectIds) {
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
}