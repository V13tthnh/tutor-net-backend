package com.tutornet.tutor_net.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder @Getter
public final class TokenResponse {
    private String accessToken;
    private String refreshToken;
}
