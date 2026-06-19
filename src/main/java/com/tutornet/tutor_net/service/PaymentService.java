package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.PaymentCreateRequest;
import com.tutornet.tutor_net.dto.response.PaymentResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;

public interface PaymentService {
    PaymentResponse createPaymentUrl(PaymentCreateRequest request, CustomUserDetails currentUser);
    String createPaymentUrlFromEmail(String contractNumber);
}
