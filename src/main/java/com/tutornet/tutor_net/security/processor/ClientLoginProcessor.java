package com.tutornet.tutor_net.security.processor;

import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.repository.UserRepository;
import com.tutornet.tutor_net.service.JwtService;
import com.tutornet.tutor_net.service.AbstractLoginProcessor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Component;

@Component
public class ClientLoginProcessor extends AbstractLoginProcessor {

    public ClientLoginProcessor(AuthenticationManager authenticationManager,
                                UserRepository userRepository,
                                JwtService jwtService) {
        super(authenticationManager, userRepository, jwtService);
    }

    @Override
    protected void verifyAccess(User user) {
        // Nếu user có bất kỳ role hệ thống nào (admin, moderator...) -> Chặn không cho login ở Client
        boolean isAdmin = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getIsSystem());

        if (isAdmin) {
            throw new RuntimeException("Tài khoản quản trị viên phải đăng nhập qua Cổng Admin.");
        }
    }
}
