package com.tutornet.tutor_net.event;

public record ContractCompletedEvent(
        Long contractId,
        String contractNumber,
        Long studentUserId,
        String studentEmail,
        String studentName,
        String tutorName
) {}
