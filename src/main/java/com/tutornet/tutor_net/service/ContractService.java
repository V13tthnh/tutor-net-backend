package com.tutornet.tutor_net.service;


import com.tutornet.tutor_net.dto.response.ContractResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface ContractService {
    ContractResponse createDraftContract(Long requestId);
    void processClickwrapSigning(Long contractId, String ipAddress);
    /**
     * Gia sư bấm xác nhận ký hợp đồng (Clickwrap).
     * Thực hiện đủ 5 bước:
     *  1. Render HTML từ Thymeleaf
     *  2. Gắn IP + signedAt vào trang PDF
     *  3. Xuất PDF bằng OpenHTMLToPDF
     *  4. Lưu file, tính SHA-256
     *  5. Cập nhật Contract → ACTIVE
     *
     * @param contractId ID hợp đồng cần ký
     * @param request    HttpServletRequest để lấy IP client
     */
    void signContract(Long contractId, HttpServletRequest request);
}
