package com.tz.forensics.config;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Google login is deliberately not considered a completed application login yet.
 * The Google identity is placed in a short-lived session challenge and the user
 * must explicitly accept the security check before an email OTP is issued.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        if (email == null || email.isBlank()) {
            response.sendRedirect("/login?error=oauth_email_missing");
            SecurityContextHolder.clearContext();
            return;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled()) || !user.isApproved() || !user.isEmailVerified()) {
            response.sendRedirect("/login?error=oauth_account_unavailable");
            SecurityContextHolder.clearContext();
            return;
        }

        
        var session = request.getSession(true);
        session.setAttribute("OAUTH_VERIFY_USER_ID", user.getId());
        session.setAttribute("OAUTH_VERIFY_EMAIL", user.getEmail());
        session.setAttribute("OAUTH_VERIFY_NAME", user.getFullName() != null ? user.getFullName() : user.getUsername());
        session.setAttribute("OAUTH_VERIFY_PICTURE", user.getProfilePicture());
        session.setAttribute("OAUTH_VERIFY_EXPIRES", System.currentTimeMillis() + 5 * 60_000L);

        // Do not allow the OAuth authentication to reach /dashboard before OTP verification.
        SecurityContextHolder.clearContext();

        response.sendRedirect("/oauth2/verify");
    }
}
