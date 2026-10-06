package com.tz.forensics.config;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class LoginAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private static final Logger log = LoggerFactory.getLogger(LoginAuthenticationFailureHandler.class);
    private static final int MAX_FAILURES = 5;
    private final UserRepository userRepository;

    public LoginAuthenticationFailureHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
        setDefaultFailureUrl("/login?error");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        if (username != null && !username.isBlank()) {
            try {
                userRepository.findByUsername(username.trim()).ifPresent(user -> {
                    int failures = user.getFailedAttempts() == null ? 0 : user.getFailedAttempts();
                    user.setFailedAttempts(failures + 1);
                    if (failures + 1 >= MAX_FAILURES) user.setAccountLocked(true);
                    userRepository.save(user);
                });
            } catch (Exception e) {
                log.error("Unable to persist login failure state: {}", e.getMessage());
            }
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
