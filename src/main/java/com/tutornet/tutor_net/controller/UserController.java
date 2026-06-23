package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.UserRequest;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.UserService;
import com.tutornet.tutor_net.service.impl.FileStorageServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileStorageServiceImpl fileStorageService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserRoleResponse.UserDetailResponse>> getUserById(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Long currentUserId = currentUser.getUser().getId();
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(currentUserId)));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<UserRoleResponse.UserDetailResponse>> updateUserProfile(
            @Valid @RequestBody UserRequest.UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Long currentUserId = currentUser.getUser().getId();
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", userService.updateUserProfile(currentUserId, request)));
    }

    @PatchMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<Void>> updateAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) throws IOException {
        Long currentUserId = currentUser.getUser().getId();

        String filePath = fileStorageService.storeAvatar(file); // /avatars/xxx.png
        userService.updateAvatar(currentUserId, new UserRequest.UpdateAvatarRequest(filePath)); // lưu path

        return ResponseEntity.ok(ApiResponse.noContent("Cập nhật Avatar thành công"));
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UserRequest.ResetPasswordRequest request
    ) {
        Long currentUserId = currentUser.getUser().getId();

        userService.resetPassword(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.noContent("Đặt lại mật khẩu thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSelfAccount(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest.DeleteAccountRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        if (!id.equals(currentUser.getUser().getId())) {
            throw new com.tutornet.tutor_net.exception.BusinessException("Bạn không có quyền xoá tài khoản của người khác.");
        }
        userService.deleteSelfAccount(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Tài khoản của bạn đã được xoá thành công.", null));
    }
}
