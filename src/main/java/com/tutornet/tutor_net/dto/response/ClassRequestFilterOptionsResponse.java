package com.tutornet.tutor_net.dto.response;

import java.util.List;

public record ClassRequestFilterOptionsResponse(
        List<StatusOption> statuses,
        List<SubjectOption> subjects,
        List<TeachingModeOption> teachingModes
) {
    public record StatusOption(String value, String label) {}
    public record SubjectOption(Long id, String name) {}
    public record TeachingModeOption(String value, String label) {}
}
