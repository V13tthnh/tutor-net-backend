package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.SubjectRequest.*;
import com.tutornet.tutor_net.dto.response.SubjectResponse;
import com.tutornet.tutor_net.entity.Subject;
import com.tutornet.tutor_net.repository.SubjectRepository;
import com.tutornet.tutor_net.service.SubjectService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;

    @Override
    @Transactional
    public SubjectResponse create(CreateSubjectRequest request) {
        if (subjectRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Slug '" + request.slug() + "' đã tồn tại");
        }

        Subject subject = Subject.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .iconUrl(request.iconUrl())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .build();

        if (request.parentId() != null) {
            Subject parent = subjectRepository.findById(request.parentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Danh mục cha không tồn tại: " + request.parentId()));
            subject.setParent(parent);
        }

        return SubjectResponse.from(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public SubjectResponse update(Long id, UpdateSubjectRequest request) {
        Subject subject = findOrThrow(id);

        // Slug không thay đổi nếu đã publish (isActive = true)
        if (!subject.getSlug().equals(request.slug())) {
            if (Boolean.TRUE.equals(subject.getIsActive())) {
                throw new IllegalStateException(
                        "Không thể đổi slug của danh mục đã được publish để tránh broken link");
            }
            if (subjectRepository.existsBySlugAndIdNot(request.slug(), id)) {
                throw new IllegalArgumentException("Slug '" + request.slug() + "' đã tồn tại");
            }
            subject.setSlug(request.slug());
        }

        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new IllegalArgumentException("Danh mục không thể là cha của chính nó");
            }
            Subject parent = subjectRepository.findById(request.parentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Danh mục cha không tồn tại: " + request.parentId()));
            subject.setParent(parent);
        } else {
            subject.setParent(null);
        }

        subject.setName(request.name());
        subject.setDescription(request.description());
        subject.setIconUrl(request.iconUrl());
        subject.setIsActive(request.isActive() != null ? request.isActive() : subject.getIsActive());
        subject.setSortOrder(request.sortOrder() != null ? request.sortOrder() : subject.getSortOrder());

        return SubjectResponse.from(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Subject subject = findOrThrow(id);
        subject.setIsActive(false);
        subjectRepository.save(subject);
    }

    @Override
    public SubjectResponse getById(Long id) {
        return SubjectResponse.from(findOrThrow(id));
    }

    @Override
    public List<SubjectResponse> search(String keyword, Boolean isActive) {
        return subjectRepository.search(keyword, isActive)
                .stream()
                .map(SubjectResponse::from)
                .toList();
    }

    @Override
    public SubjectResponse getBySlug(String slug) {
        return subjectRepository.findBySlug(slug)
                .map(SubjectResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy slug: " + slug));
    }

    @Override
    public List<SubjectResponse> getAll() {
        return subjectRepository.findAll(Sort.by("sortOrder")).stream()
                .map(SubjectResponse::from)
                .toList();
    }

    @Override
    public List<SubjectResponse> getTree() {
        // 1 query duy nhất, build cây trong memory → tránh N+1
        List<Subject> all = subjectRepository.findAllActive();

        Map<Long, SubjectResponse> map = new LinkedHashMap<>();
        List<SubjectResponse> roots = new ArrayList<>();

        // tạo tất cả node
        for (Subject s : all) {
            map.put(s.getId(), SubjectResponse.from(s)); // không include children
        }

        // gắn children vào parent
        for (Subject s : all) {
            if (s.getParent() == null) {
                roots.add(map.get(s.getId()));
            } else {
                SubjectResponse parent = map.get(s.getParent().getId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(map.get(s.getId()));
                }
            }
        }

        return roots;
    }


    @Override
    public List<SubjectResponse> getChildren(Long parentId) {
        findOrThrow(parentId); // validate parent tồn tại
        return subjectRepository.findByParentId(parentId, Sort.by("sortOrder")).stream()
                .map(SubjectResponse::from)
                .toList();
    }


    @Override
    @Transactional
    public SubjectResponse reorder(Long id, SubjectReorderRequest request) {
        Subject subject = findOrThrow(id);

        // Validate không tự làm cha của chính mình
        if (request.parentId() != null && request.parentId().equals(id)) {
            throw new IllegalArgumentException("Danh mục không thể là cha của chính nó");
        }

        // Cập nhật parent
        if (request.parentId() != null) {
            Subject newParent = subjectRepository.findById(request.parentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Danh mục cha không tồn tại: " + request.parentId()));

            // Validate không tạo vòng lặp (A → B → A)
            if (isDescendant(newParent, id)) {
                throw new IllegalArgumentException(
                        "Không thể chuyển danh mục vào một danh mục con của chính nó");
            }

            subject.setParent(newParent);
        } else {
            subject.setParent(null); // chuyển lên root
        }

        // Dịch chuyển sortOrder của các subject cùng cấp
        List<Subject> siblings = (request.parentId() == null
                ? subjectRepository.findByParentIsNull(Sort.by("sortOrder"))
                : subjectRepository.findByParentId(request.parentId(), Sort.by("sortOrder")))
                .stream()
                .filter(s -> !s.getId().equals(id))
                .collect(Collectors.toCollection(ArrayList::new));

        // Chèn subject vào đúng vị trí, đẩy các item phía sau lên +1
        siblings.add(request.sortOrder(), subject);
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setSortOrder(i);
        }
        subjectRepository.saveAll(siblings);

        subject.setSortOrder(request.sortOrder());
        return SubjectResponse.from(subjectRepository.save(subject));
    }

    // Kiểm tra newParent có phải là hậu duệ của subject không
    private boolean isDescendant(Subject target, Long ancestorId) {
        Subject current = target;
        while (current.getParent() != null) {
            if (current.getParent().getId().equals(ancestorId)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Subject subject = findOrThrow(id);

        // Không xoá nếu có subject con
        if (subjectRepository.existsByParentId(id)) {
            throw new IllegalStateException(
                    "Không thể xoá danh mục đang có danh mục con");
        }

        // Không xoá nếu đang được tutor/student dùng
        // (bật lại sau khi có bảng liên quan)
//         if (subjectRepository.existsInUse(id)) {
//             throw new IllegalStateException(
//                 "Không thể xoá danh mục đang được sử dụng");
//         }

        subjectRepository.deleteById(id);
    }

    // ── Helper ────────────────────────────────────────────────────────────
    private Subject findOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục: " + id));
    }
}
