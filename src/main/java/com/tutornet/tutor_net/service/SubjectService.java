package com.tutornet.tutor_net.service;


import com.tutornet.tutor_net.dto.request.SubjectRequest.*;
import com.tutornet.tutor_net.dto.response.SubjectResponse;

import java.util.List;

public interface SubjectService {

    /** Tạo mới danh mục */
    SubjectResponse create(CreateSubjectRequest request);

    /** Cập nhật danh mục (slug không đổi sau khi publish — service sẽ validate) */
    SubjectResponse update(Long id, UpdateSubjectRequest request);

    /** Soft disable thay vì DELETE */
    void deactivate(Long id);

    /** Lấy theo ID */
    SubjectResponse getById(Long id);

    List<SubjectResponse> search(String keyword, Boolean isActive);

    /** Lấy theo slug */
    SubjectResponse getBySlug(String slug);

    /** Lấy danh sách phẳng tất cả danh mục (admin) */
    List<SubjectResponse> getAll();

    /** Lấy cây danh mục đầy đủ (root → children → grandchildren) */
    List<SubjectResponse> getTree();

    /** Lấy danh mục con trực tiếp của parent */
    List<SubjectResponse> getChildren(Long parentId);

    SubjectResponse reorder(Long id, SubjectReorderRequest request);

    void delete(Long id);
}
