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

    @Transactional
    public PaymentResponse processPayment(Contract contract, User user, String ipAddress) {
        Transaction txnToProcess;

        Optional<Transaction> existingPendingTxn = transactionRepository
                .findTopByContractIdAndStatusOrderByCreatedAtDesc(contract.getId(), TransactionStatus.PENDING);

        if (existingPendingTxn.isPresent()) {
            txnToProcess = existingPendingTxn.get();
        } else {
            String transactionCode = "TXN" + System.currentTimeMillis();

            Transaction newTxn = Transaction.builder()
                    .contract(contract)
                    .user(user)
                    .amount(contract.getIntroductionFee())
                    .paymentMethod(getPaymentMethod())
                    .status(TransactionStatus.PENDING)
                    .transactionCode(transactionCode)
                    .build();

            txnToProcess = transactionRepository.save(newTxn);
        }

        String checkoutUrl = buildCheckoutUrl(txnToProcess, ipAddress);

        return PaymentResponse.builder()
                .transactionCode(txnToProcess.getTransactionCode())
                .checkoutUrl(checkoutUrl)
                .build();
    }

    protected abstract PaymentMethod getPaymentMethod();

    protected abstract String buildCheckoutUrl(Transaction txn, String ipAddress);
}