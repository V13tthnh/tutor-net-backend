package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.SubjectRequest.*;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.SubjectResponse;
import com.tutornet.tutor_net.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/subjects")
@RequiredArgsConstructor
public class SubjectAdminController {

    private final SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasAuthority('subject:read')")
    public ResponseEntity<ApiResponse<SubjectResponse>> create(@Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(subjectService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('subject:update')")
    public ResponseEntity<ApiResponse<SubjectResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", subjectService.update(id, request)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('subject:toggle_active')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        subjectService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('subject:read')")
    public ResponseEntity<ApiResponse<SubjectResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('subject:read')")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive
    ) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.search(keyword, isActive)));
    }

    @GetMapping("tree")
    @PreAuthorize("hasAuthority('subject:manage_tree')")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getTree() {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.getTree()));
    }

    @PatchMapping("/{id}/reorder")
    @PreAuthorize("hasAuthority('subject:reorder')")
    public ResponseEntity<ApiResponse<SubjectResponse>> reorder(
            @PathVariable Long id,
            @Valid @RequestBody SubjectReorderRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.reorder(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('subject:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}