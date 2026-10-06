package com.tz.forensics.config;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Component
public class LoginAuthenticationSuccessListener {
    private static final Logger log = LoggerFactory.getLogger(LoginAuthenticationSuccessListener.class);
    private final UserRepository userRepository;

    public LoginAuthenticationSuccessListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication == null || authentication.getName() == null) return;
        try {
            userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
                user.setFailedAttempts(0);
                user.setAccountLocked(false);
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
            });
        } catch (Exception e) {
            log.error("Unable to persist login success state: {}", e.getMessage());
        }
    }
}
