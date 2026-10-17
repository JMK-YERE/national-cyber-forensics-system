package com.tz.forensics.config;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Prevents newly provisioned accounts from accessing application pages before changing their temporary password. */
public class FirstLoginPasswordFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;

    public FirstLoginPasswordFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/change-password")
                || path.equals("/logout")
                || path.equals("/login")
                || path.equals("/error")
                || path.equals("/access-denied")
                || path.equals("/health")
                || path.equals("/favicon.ico")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/static/")
                || path.startsWith("/webjars/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/set-password")
                || path.startsWith("/forgot-password")
                || path.startsWith("/reset-password")
                || path.startsWith("/verify-email");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            User user = userRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null && Boolean.TRUE.equals(user.getMustChangePassword())) {
                response.sendRedirect(request.getContextPath() + "/change-password?firstLogin=true");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
