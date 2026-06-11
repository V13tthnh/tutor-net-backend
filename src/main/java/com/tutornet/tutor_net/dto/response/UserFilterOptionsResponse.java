package com.tutornet.tutor_net.dto.response;

import java.util.List;

public record UserFilterOptionsResponse(
        List<StatusOption> statuses,
        List<RoleOption> roles
) {
    public record StatusOption(String value, String label) {}
    public record RoleOption(Long id, String slug, String name) {}
}