package com.tutornet.tutor_net.repository.projection;

// Hứng kết quả Query Nhóm theo Danh mục (Trạng thái/Môn học)
public interface CategoryCountProjection {
    String getCategoryName();
    Long getCount();
}
