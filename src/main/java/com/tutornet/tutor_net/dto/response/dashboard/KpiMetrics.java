package com.tutornet.tutor_net.dto.response.dashboard;

import java.math.BigDecimal;

// 1. DTO hiển thị 4 KPI đầu trang
public record KpiMetrics(
        BigDecimal totalRevenue,
        long totalClassRequests,
        double matchRate, // Phần trăm lớp MATCHED / Tổng lớp
        long newTutors,
        long pendingTutorsCount // Bổ sung số lượng gia sư đang chờ duyệt
) {}
