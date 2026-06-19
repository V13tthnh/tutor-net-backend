package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.TransactionFilterRequest;
import com.tutornet.tutor_net.dto.response.TransactionResponse;
import com.tutornet.tutor_net.dto.response.TransactionSummaryResponse;
import org.springframework.data.domain.Page;

public interface TransactionService {

    /**
     * Danh sách giao dịch có phân trang + lọc — dành cho Admin.
     */
    Page<TransactionResponse> getTransactions(TransactionFilterRequest filter);

    /**
     * KPI tổng hợp theo cùng bộ lọc (hiển thị trên header bảng).
     */
    TransactionSummaryResponse getSummary(TransactionFilterRequest filter);

    /**
     * Chi tiết 1 giao dịch.
     */
    TransactionResponse getById(Long id);
}