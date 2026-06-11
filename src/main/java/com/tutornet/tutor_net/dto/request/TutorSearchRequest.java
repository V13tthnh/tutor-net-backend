package com.tutornet.tutor_net.dto.request;


import com.tutornet.tutor_net.enums.GenderType;
import com.tutornet.tutor_net.enums.TeachingMode;

import java.util.List;

public final class TutorSearchRequest {

    public record SearchFilter(
            String keyword,                  // tìm theo tên
            List<Long> subjectIds,           // lọc theo môn học
            List<String> provinces,          // lọc theo tỉnh/thành phố
            List<GenderType> genders,        // lọc theo giới tính
            List<TeachingMode> teachingModes // lọc theo hình thức dạy
    ) {}
}

