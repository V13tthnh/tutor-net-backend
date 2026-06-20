package com.tutornet.tutor_net.payment;

import com.tutornet.tutor_net.dto.response.PaymentResponse;
import com.tutornet.tutor_net.entity.Contract;
import com.tutornet.tutor_net.entity.Transaction;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.enums.PaymentMethod;
import com.tutornet.tutor_net.enums.TransactionStatus;
import com.tutornet.tutor_net.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
public abstract class BasePaymentProcessor {

    protected final TransactionRepository transactionRepository;

    // TEMPLATE METHOD
    @Transactional
    public PaymentResponse processPayment(Contract contract, User user, String ipAddress) {
        Transaction txnToProcess;

        // KIỂM TRA CHỐNG RÁC DB: Xem đã có giao dịch PENDING nào của hợp đồng này chưa
        Optional<Transaction> existingPendingTxn = transactionRepository
                .findTopByContractIdAndStatusOrderByCreatedAtDesc(contract.getId(), TransactionStatus.PENDING);

        if (existingPendingTxn.isPresent()) {
            // Nếu có rồi thì lôi ra xài lại, không tạo dòng mới
            txnToProcess = existingPendingTxn.get();
        } else {
            // Nếu chưa có thì mới khởi tạo
            String transactionCode = "TXN" + System.currentTimeMillis();

            Transaction newTxn = Transaction.builder()
                    .contract(contract)
                    .user(user)
                    .amount(contract.getIntroductionFee())
                    .paymentMethod(getPaymentMethod()) // Lấy từ lớp con
                    .status(TransactionStatus.PENDING)
                    .transactionCode(transactionCode)
                    .build();

            txnToProcess = transactionRepository.save(newTxn);
        }

        // Uỷ quyền cho lớp con (VNPay) tự build Link thanh toán với mã TXN tương ứng
        String checkoutUrl = buildCheckoutUrl(txnToProcess, ipAddress);

        return PaymentResponse.builder()
                .transactionCode(txnToProcess.getTransactionCode())
                .checkoutUrl(checkoutUrl)
                .build();
    }

    // Các hàm lớp con bắt buộc phải ghi đè
    protected abstract PaymentMethod getPaymentMethod();

    protected abstract String buildCheckoutUrl(Transaction txn, String ipAddress);
}