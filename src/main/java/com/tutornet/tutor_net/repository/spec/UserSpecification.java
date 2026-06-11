package com.tutornet.tutor_net.repository.spec;

import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.enums.UserStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("fullName")), pattern)
            );
        };
    }

    public static Specification<User> hasStatuses(List<UserStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) return cb.conjunction();
            return root.get("status").in(statuses);
        };
    }

    public static Specification<User> hasRoleSlugs(List<String> roleSlugs) {
        return (root, query, cb) -> {
            if (roleSlugs == null || roleSlugs.isEmpty()) return cb.conjunction();
            Join<Object, Object> userRoles = root.join("userRoles", JoinType.LEFT);
            Join<Object, Object> role = userRoles.join("role", JoinType.LEFT);
            return role.get("slug").in(roleSlugs);
        };
    }

    public static Specification<User> hasAdminRoleIds(List<Long> adminRoleIds) {
        return (root, query, cb) -> {
            Join<Object, Object> userRoles = root.join("userRoles", JoinType.LEFT);
            Join<Object, Object> role = userRoles.join("role", JoinType.LEFT);
            query.distinct(true);
            return role.get("id").in(adminRoleIds);
        };
    }
}