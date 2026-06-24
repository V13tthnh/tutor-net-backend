package com.tutornet.tutor_net.mail.payload;

public record ContractAttachmentPayload(String recipientName, String contractNumber, byte[] pdfBytes, boolean isTutor) {}
