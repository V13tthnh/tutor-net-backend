package com.tutornet.tutor_net.mapper;

import com.tutornet.tutor_net.dto.response.TransactionResponse;
import com.tutornet.tutor_net.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getTransactionCode(),
                t.getGatewayReference(),

                // Contract
                t.getContract() != null ? t.getContract().getId() : null,
                t.getContract() != null ? t.getContract().getContractNumber() : null,

                // User (gia sư thanh toán)
                t.getUser() != null ? t.getUser().getId() : null,
                t.getUser() != null ? t.getUser().getFullName() : null,
                t.getUser() != null ? t.getUser().getEmail() : null,

                t.getAmount(),
                t.getPaymentMethod(),
                t.getStatus(),
                t.getNote(),
                t.getPaidAt(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}