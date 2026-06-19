package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.config.VNPayConfig;
import com.tutornet.tutor_net.dto.request.PaymentCreateRequest;
import com.tutornet.tutor_net.dto.response.PaymentResponse;
import com.tutornet.tutor_net.entity.Contract;
import com.tutornet.tutor_net.entity.Transaction;
import com.tutornet.tutor_net.enums.PaymentMethod;
import com.tutornet.tutor_net.enums.TransactionStatus;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.repository.ContractRepository;
import com.tutornet.tutor_net.repository.TransactionRepository;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final ContractRepository contractRepository;
    private final TransactionRepository transactionRepository;
    private final VNPayConfig vnPayConfig;

    @Override
    @Transactional
    public PaymentResponse createPaymentUrl(PaymentCreateRequest request, CustomUserDetails currentUser) {
        Contract contract = contractRepository.findById(request.contractId())
                .orElseThrow(() -> new ResourceNotFoundException("Hợp đồng không tồn tại"));

        if (!contract.getTutor().getUser().getId().equals(currentUser.getUser().getId())) {
            throw new BusinessException("Bạn không có quyền thanh toán cho hợp đồng này.");
        }
        if (Boolean.TRUE.equals(contract.getIsFeePaid())) {
            throw new BusinessException("Hợp đồng này đã được thanh toán.");
        }

        // Tạo mã giao dịch
        String transactionCode = "TXN" + System.currentTimeMillis();

        Transaction newTxn = Transaction.builder()
                .contract(contract)
                .user(currentUser.getUser())
                .amount(contract.getIntroductionFee())
                .paymentMethod(PaymentMethod.VNPAY)
                .status(TransactionStatus.PENDING)
                .transactionCode(transactionCode)
                .build();
        transactionRepository.save(newTxn);

        // XÂY DỰNG LINK VNPAY
        long amount = contract.getIntroductionFee().longValue() * 100L; // VNPay yêu cầu nhân 100

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnPayConfig.vnp_Version);
        vnp_Params.put("vnp_Command", vnPayConfig.vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnPayConfig.vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", transactionCode);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan HD " + contract.getContractNumber());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", "127.0.0.1"); // Có thể lấy thực tế từ HttpServletRequest

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15); // Hóa đơn hết hạn sau 15 phút
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Sắp xếp các tham số theo thứ tự alphabet
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        try {
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                    // Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Lỗi khởi tạo link thanh toán");
        }

        // Băm chữ ký điện tử
        String queryUrl = query.toString();
        String vnp_SecureHash = vnPayConfig.hmacSHA512(vnPayConfig.secretKey, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnPayConfig.vnp_PayUrl + "?" + queryUrl;

        return PaymentResponse.builder()
                .transactionCode(transactionCode)
                .checkoutUrl(paymentUrl)
                .build();
    }

    @Override
    @Transactional
    public String createPaymentUrlFromEmail(String contractNumber) {
        // 1. Tìm hợp đồng dựa vào mã số bảo mật gửi từ Email
        Contract contract = contractRepository.findByContractNumber(contractNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng: " + contractNumber));

        if (Boolean.TRUE.equals(contract.getIsFeePaid())) {
            throw new BusinessException("Hợp đồng này đã được hoàn thành thanh toán trước đó.");
        }

        // 2. Tạo một bản ghi giao dịch PENDING mới cho lần bấm này
        String transactionCode = "TXN" + System.currentTimeMillis();
        Transaction newTxn = Transaction.builder()
                .contract(contract)
                .user(contract.getTutor().getUser()) // Lấy luôn user gắn với gia sư của hợp đồng
                .amount(contract.getIntroductionFee())
                .paymentMethod(PaymentMethod.VNPAY)
                .status(TransactionStatus.PENDING)
                .transactionCode(transactionCode)
                .build();
        transactionRepository.save(newTxn);

        // 3. Re-use (Tái sử dụng) logic cấu hình tham số VNPay của bạn
        long amount = contract.getIntroductionFee().longValue() * 100L;
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnPayConfig.vnp_Version);
        vnp_Params.put("vnp_Command", vnPayConfig.vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnPayConfig.vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", transactionCode);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan tu dong HD " + contract.getContractNumber());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15); // Link mới này sẽ có 15 phút hiệu lực để quét mã
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // --- Tiến hành Sort và build chuỗi URL mã hóa bảo mật ---
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        try {
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Lỗi cấu trúc dữ liệu cổng thanh toán");
        }

        String vnp_SecureHash = vnPayConfig.hmacSHA512(vnPayConfig.secretKey, hashData.toString());
        return vnPayConfig.vnp_PayUrl + "?" + query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;
    }
}
