package com.tutornet.tutor_net.event;

public record ContractSignedEvent(
        String contractNumber,
        String tutorEmail,
        String tutorName,
        String studentEmail,
        String studentName,
        byte[] pdfBytes // Gửi kèm mảng byte để đính kèm vào Mail luôn, cực kỳ tối ưu hiệu năng
) {}