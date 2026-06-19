package com.tutornet.tutor_net.dto.response;

import java.math.BigDecimal;

/**
 * KPI tổng hợp hiển thị trên đầu trang quản lý giao dịch.
 * Tính theo khoảng thời gian đang filter.
 */
public record TransactionSummaryResponse(
        long totalCount,             // Tổng số giao dịch
        long successCount,           // Số giao dịch thành công
        long pendingCount,           // Số giao dịch đang chờ
        long failedCount,            // Số giao dịch thất bại
        BigDecimal totalRevenue      // Tổng doanh thu (chỉ tính SUCCESS)
) {}

