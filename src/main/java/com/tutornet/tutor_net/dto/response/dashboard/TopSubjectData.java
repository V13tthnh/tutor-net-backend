package com.tutornet.tutor_net.dto.response.dashboard;

public record TopSubjectData(
        String categoryName, // Tên môn học hoặc Trạng thái
        long count
) {}
