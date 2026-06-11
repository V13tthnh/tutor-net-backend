package com.tutornet.tutor_net.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class UserResponse {

    private Long id;

    private String email;

    private String fullName;

    private String avatarUrl;

    private List<String> roles;

    private List<String> permissions;

    public record AdminUserResponse(
            Long id,

            String email,

            String fullName,

            String avatarUrl,

            List<String> roles,

            List<String> permissions
    ){}

    public record UserProfileResponse(
            Long id,

            String email,

            String fullName,

            String avatarUrl,

            List<String> roles,

            List<String> permissions
    ){}
}