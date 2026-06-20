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
public class ClientLoginProcessor extends AbstractLoginProcessor {

    public ClientLoginProcessor(AuthenticationManager authenticationManager,
                                UserRepository userRepository,
                                JwtService jwtService,
                                RateLimiterService rateLimiterService) {
        super(authenticationManager, userRepository, jwtService, rateLimiterService);
    }

    @Override
    protected void verifyAccess(User user) {
        // Nếu user có bất kỳ role hệ thống nào (admin, moderator...) thì chặn không cho login ở Client
        boolean isAdmin = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getIsSystem());

        if (isAdmin) {
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác");
        }
    }
}
