package com.tutornet.tutor_net.dto.request;

import jakarta.validation.constraints.*;

public final class SubjectRequest {

    private SubjectRequest() {
    }

    public record CreateSubjectRequest(
            Long parentId,

            @NotBlank(message = "Tên môn học không được để trống")
            @Size(max = 200, message = "Tên không vượt quá 200 ký tự")
            String name,

            @NotBlank(message = "Slug không được để trống")
            @Size(max = 200, message = "Slug không vượt quá 200 ký tự")
            @Pattern(
                    regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                    message = "Slug chỉ gồm chữ thường, số và dấu gạch ngang (ví dụ: toan-hoc)"
            )
            String slug,

            @Size(max = 5000, message = "Mô tả không vượt quá 5000 ký tự")
            String description,

            String iconUrl,

            Boolean isActive,

            @Min(value = 0, message = "Thứ tự hiển thị phải >= 0")
            Integer sortOrder
    ) {}

    public record UpdateSubjectRequest(
            Long parentId,

            @NotBlank(message = "Tên môn học không được để trống")
            @Size(max = 200, message = "Tên không vượt quá 200 ký tự")
            String name,

            @NotBlank(message = "Slug không được để trống")
            @Size(max = 200, message = "Slug không vượt quá 200 ký tự")
            @Pattern(
                    regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                    message = "Slug chỉ gồm chữ thường, số và dấu gạch ngang (ví dụ: toan-hoc)"
            )
            String slug,

            @Size(max = 5000, message = "Mô tả không vượt quá 5000 ký tự")
            String description,

            @Size(max = 2048, message = "URL icon không được vượt quá 2048 ký tự")
            String iconUrl,

            Boolean isActive,

            @Min(value = 0, message = "Thứ tự hiển thị phải >= 0")
            Integer sortOrder
    ) {}

    public record SubjectReorderRequest(
             Long parentId,   // null = chuyển lên root

             @NotNull
             @Min(0)
             Integer sortOrder
    ){}
}
