package com.tz.forensics.config;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class UnreadNotificationModelAdvice {
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public UnreadNotificationModelAdvice(UserRepository userRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @ModelAttribute
    public void addUnreadNotificationCount(Model model) {
        long count = 0;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                User user = userRepository.findByUsername(auth.getName()).orElse(null);
                if (user != null && user.getId() != null) count = notificationService.getUnreadCount(user.getId());
            }
        } catch (RuntimeException ignored) {
            // Notifications must never prevent the requested page from loading.
        }
        model.addAttribute("unreadNotificationCount", count);
    }
}
