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
import com.tutornet.tutor_net.payment.VNPayProcessor;
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
    private final VNPayProcessor vnPayProcessor;

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

        return vnPayProcessor.processPayment(contract, currentUser.getUser(), "127.0.0.1");
    }

    @Override
    @Transactional
    public String createPaymentUrlFromEmail(String contractNumber) {
        // Tìm hợp đồng dựa vào mã số bảo mật gửi từ Email
        Contract contract = contractRepository.findFirstByContractNumber(contractNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng: " + contractNumber));

        if (Boolean.TRUE.equals(contract.getIsFeePaid())) {
            throw new BusinessException("Hợp đồng này đã được hoàn thành thanh toán trước đó.");
        }

        // Tạo một bản ghi giao dịch PENDING mới cho lần bấm này
        PaymentResponse response = vnPayProcessor.processPayment(contract, contract.getTutor().getUser(), "127.0.0.1");
        return response.checkoutUrl();
    }
}
