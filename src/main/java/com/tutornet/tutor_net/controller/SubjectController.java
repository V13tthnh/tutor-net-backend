package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.SubjectResponse;
import com.tutornet.tutor_net.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getTree() {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.getTree()));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<SubjectResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.getBySlug(slug)));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getChildren(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.getChildren(id)));
    }
}
