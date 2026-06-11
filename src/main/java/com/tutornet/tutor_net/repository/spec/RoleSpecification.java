package com.tutornet.tutor_net.repository.spec;

import com.tutornet.tutor_net.entity.Role;
import org.springframework.data.jpa.domain.Specification;

public class RoleSpecification {
    public static Specification<Role> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("slug")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Role> isSystem(Boolean isSystem) {
        return (root, query, cb) -> {
            if (isSystem == null) return null;
            return cb.equal(root.get("isSystem"), isSystem);
        };
    }
}
