package com.tz.forensics.config;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LoginAuthenticationSuccessListener {
    private final UserRepository userRepository;

    public LoginAuthenticationSuccessListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication == null || authentication.getName() == null) return;
        userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
            user.setFailedAttempts(0);
            user.setAccountLocked(false);
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }
}
