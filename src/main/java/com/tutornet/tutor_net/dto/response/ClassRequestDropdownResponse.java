package com.tutornet.tutor_net.dto.response;

import java.math.BigDecimal;

public record ClassRequestDropdownResponse(
        Long id,
        String classCode,
        String subjectName,
        String gradeLevel,
        BigDecimal proposedPrice
) {}
