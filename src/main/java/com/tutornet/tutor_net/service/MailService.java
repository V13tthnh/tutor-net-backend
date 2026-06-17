package com.tutornet.tutor_net.service;

public interface MailService {
    void sendVerificationEmail(String toEmail, String token);
    void sendPasswordResetEmail(String toEmail, String fullName, String token);

    /* Gửi mail thông báo cập nhật thông tin cho gia sư */
    void sendTutorApprovedEmail(String toEmail, String fullName);
    void sendTutorRejectedEmail(String toEmail, String fullName, String reason);

    // Gửi mail đăng lớp học
    void sendClassRequestConfirmationEmail(String toEmail, String studentName, String subjectName);

    // Gửi mail từ chối đăng lớp học
    void sendClassRequestRejectedEmail(String toEmail, String contactName, String subjectName, String rejectionReason);

    // Gửi mail chấp nhận đăng lớp học
    void sendClassRequestApprovedEmail(String toEmail, String contactName, String subjectName);

    // gửi mail mời dạy
    void sendTutorDirectInviteEmail(String tutorEmail, String tutorName, String subjectName);

    // gửi mail gia sư ứng tuyển
    void sendTutorAppliedEmail(String toEmail, String studentName, String tutorName);

    // gửi mail gia sư được học viên mời dạy
    void sendTutorInvitedEmail(String toEmail, String tutorName, String studentName, String message);

    // gửi mail báo gia sư đã chấp nhận lời mời dạy
    void sendTutorAcceptedInvitationEmail(String toEmail,
                                          String studentName,
                                          String tutorName);

    void sendContractAttachmentEmail(String toEmail, String recipientName, String contractNumber, byte[] pdfBytes);
    void sendTutorApplicationAcceptedEmail(String toEmail, String tutorName, String studentName);
    void sendApplicationRejectedByAdminEmail(String toEmail, String tutorName, String contactName);

}
