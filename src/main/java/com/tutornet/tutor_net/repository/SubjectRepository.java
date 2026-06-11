package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.Subject;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<Subject> findBySlug(String slug);

    // Tất cả danh mục gốc (cấp 1), sắp xếp theo sortOrder
    List<Subject> findByParentIsNull(Sort sort);

    @Query("""
    SELECT s FROM Subject s
    WHERE (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND   (:isActive IS NULL OR s.isActive = :isActive)
    ORDER BY s.sortOrder ASC
    """)
    List<Subject> search(@Param("keyword") String keyword,
                         @Param("isActive") Boolean isActive);

    // Danh mục con trực tiếp của parent
    List<Subject> findByParentId(Long parentId, Sort sort);

    // Lấy toàn bộ danh mục active, dùng để build cây ở service layer
    @Query("SELECT s FROM Subject s WHERE s.isActive = true ORDER BY s.sortOrder ASC")
    List<Subject> findAllActive();

    // Kiểm tra có danh mục con không (trước khi xoá/deactivate)
    boolean existsByParentId(Long parentId);

    // Kiểm tra subject có đang được dùng không (thêm sau khi có entity liên quan)
//    @Query("SELECT COUNT(t) > 0 FROM TutorSubject t WHERE t.subject.id = :id")
//    boolean existsInUse(@Param("id") Long id);
}
