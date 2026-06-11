package com.tutornet.tutor_net.mapper;

import com.tutornet.tutor_net.dto.response.TutorInvitationResponse;
import com.tutornet.tutor_net.entity.TutorInvitation;
import com.tutornet.tutor_net.enums.InvitationStatus;
import org.springframework.stereotype.Component;

@Component
public class TutorInvitationMapper {

    /**
     * Chuyển entity → DTO.
     * Nếu lời mời chưa được chấp nhận (khác ACCEPTED) thì mask
     * số điện thoại và email để bảo vệ thông tin học viên.
     */
    public TutorInvitationResponse toResponse(TutorInvitation invitation) {
        boolean revealed = InvitationStatus.ACCEPTED.equals(invitation.getStatus());

        return TutorInvitationResponse.builder()
                .id(invitation.getId())
                .studentUserId(invitation.getStudentUserId())
                .studentName(invitation.getStudentName())
                .studentPhone(revealed
                        ? invitation.getStudentPhone()
                        : maskPhone(invitation.getStudentPhone()))
                .studentEmail(revealed
                        ? invitation.getStudentEmail()
                        : maskEmail(invitation.getStudentEmail()))
                .message(invitation.getMessage())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .build();
    }

    // ---------------------------------------------------------------
    // Masking helpers
    // ---------------------------------------------------------------

    /**
     * Mask số điện thoại: giữ 3 số đầu và 2 số cuối, che giữa bằng *.
     * Ví dụ:  0912345678  →  091*****78
     *         +84912345678 →  +84*****78  (prefix không phải digit được giữ)
     */
    static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return phone;

        // Tách prefix không phải chữ số (vd: +84)
        int digitStart = 0;
        while (digitStart < phone.length() && !Character.isDigit(phone.charAt(digitStart))) {
            digitStart++;
        }

        String prefix = phone.substring(0, digitStart);   // vd: "+84" hoặc ""
        String digits = phone.substring(digitStart);       // phần chỉ có chữ số

        if (digits.length() <= 5) {
            // Quá ngắn → che hết phần giữa, giữ ký tự đầu và cuối
            return prefix + digits.charAt(0) + "*".repeat(Math.max(digits.length() - 2, 1))
                    + (digits.length() > 1 ? digits.charAt(digits.length() - 1) : "");
        }

        String head = digits.substring(0, 3);                        // 3 số đầu
        String tail = digits.substring(digits.length() - 2);         // 2 số cuối
        String mask = "*".repeat(digits.length() - 5);               // che phần giữa

        return prefix + head + mask + tail;
    }

    /**
     * Mask email: che phần local-part, giữ 2 ký tự đầu và ký tự cuối trước @.
     * Phần domain giữ nguyên.
     * Ví dụ:  nguyenvana@gmail.com  →  ng*******a@gmail.com
     *         ab@yahoo.com          →  a*b@yahoo.com
     *         a@outlook.com         →  a**@outlook.com
     */
    static String maskEmail(String email) {
        if (email == null || email.isBlank()) return email;

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return email;   // Không hợp lệ → trả nguyên

        String local  = email.substring(0, atIndex);
        String domain = email.substring(atIndex);     // "@gmail.com"

        String maskedLocal;
        if (local.length() <= 2) {
            // "ab" → "a*"
            maskedLocal = local.charAt(0) + "*".repeat(local.length() - 1);
        } else {
            // Giữ 2 đầu + 1 cuối, che giữa
            String head   = local.substring(0, 2);
            String tail   = local.substring(local.length() - 1);
            String middle = "*".repeat(local.length() - 3);
            maskedLocal = head + middle + tail;
        }

        return maskedLocal + domain;
    }
}