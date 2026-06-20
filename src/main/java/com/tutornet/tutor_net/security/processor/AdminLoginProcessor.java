package com.tutornet.tutor_net.security.processor;

import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.repository.UserRepository;
import com.tutornet.tutor_net.service.JwtService;
import com.tutornet.tutor_net.service.AbstractLoginProcessor;
import com.tutornet.tutor_net.service.RateLimiterService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class AdminLoginProcessor extends AbstractLoginProcessor {

    public AdminLoginProcessor(AuthenticationManager authenticationManager,
                               UserRepository userRepository,
                               JwtService jwtService,
                               RateLimiterService rateLimiterService) {
        super(authenticationManager, userRepository, jwtService, rateLimiterService);
    }

    @Override
    protected void verifyAccess(User user) {
        // Bắt buộc phải có quyền hệ thống mới được vào trang quản trị
        boolean hasAdminAccess = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getIsSystem());

        if (!hasAdminAccess) {
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác");
        }
    }
}
