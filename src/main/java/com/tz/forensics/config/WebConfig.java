package com.tz.forensics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.HandlerInterceptor;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import java.time.Duration;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final UserRepository userRepository;

    public WebConfig(UserRepository userRepository) { this.userRepository = userRepository; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                if (path.equals("/change-password") || path.equals("/logout") || path.equals("/error")
                        || path.equals("/forgot-password") || path.startsWith("/forgot-password/")
                        || path.equals("/reset-password") || path.equals("/set-password")
                        || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
                        || path.startsWith("/webjars/") || path.equals("/favicon.ico")) return true;
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()
                        || "anonymousUser".equals(authentication.getPrincipal())) return true;
                User user = userRepository.findByUsername(authentication.getName()).orElse(null);
                if (user == null || !"INIT".equalsIgnoreCase(user.getPasswordSetupChannel())) return true;
                if (user.getPasswordSetupExpiresAt() == null || user.getPasswordSetupExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                    response.sendRedirect(request.getContextPath() + "/forgot-password?temporaryExpired=true");
                } else {
                    response.sendRedirect(request.getContextPath() + "/change-password?required=true");
                }
                return false;
            }
        });
    }

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("lang");
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }
}
