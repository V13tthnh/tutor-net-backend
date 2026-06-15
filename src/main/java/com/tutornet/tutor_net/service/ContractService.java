package com.tutornet.tutor_net.service;


import com.tutornet.tutor_net.dto.response.ContractResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface ContractService {
    ContractResponse createDraftContract(Long requestId);
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
     * @param ipAddress địa chỉ ip
     */
    void signContractAndGeneratePdf(Long contractId, String ipAddress);
}
