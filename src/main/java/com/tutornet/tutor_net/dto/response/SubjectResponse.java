package com.tutornet.tutor_net.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tutornet.tutor_net.entity.Subject;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SubjectResponse {

    private Long id;
    private Long parentId;
    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private Boolean isActive;
    private Integer sortOrder;
    private OffsetDateTime createdAt;

    // Trả về cây con khi cần (null nếu không query tree)
    private List<SubjectResponse> children;

    // ── Mapper tĩnh (không cần thư viện thêm) ───────────────────────────
    public static SubjectResponse from(Subject entity) {
        return SubjectResponse.builder()
                .id(entity.getId())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .name(entity.getName())
                .slug(entity.getSlug())
                .description(entity.getDescription())
                .iconUrl(entity.getIconUrl())
                .isActive(entity.getIsActive())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static SubjectResponse fromWithChildren(Subject entity) {
        SubjectResponse response = from(entity);
        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            response.setChildren(
                    entity.getChildren().stream()
                            .map(SubjectResponse::fromWithChildren)
                            .toList()
            );
        }
        return response;
    }
}
