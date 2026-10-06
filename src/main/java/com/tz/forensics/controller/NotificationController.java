package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public NotificationController(NotificationService notificationService,
                                  UserRepository userRepository,
                                  AuditService auditService) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private User currentUser(Authentication auth) {
        return auth == null ? null : userRepository.findByUsername(auth.getName()).orElse(null);
    }

    @GetMapping
    public String listNotifications(Authentication auth, Model model) {
        User user = currentUser(auth);
        if (user == null) return "redirect:/login";

        model.addAttribute("notifications", notificationService.getNotificationsForUser(user.getId()));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user.getId()));
        return "notifications";
    }

    @PostMapping("/read/{id}")
    public String markAsRead(@PathVariable Long id, Authentication auth) {
        User user = currentUser(auth);
        if (user == null) return "redirect:/login";

        if (!notificationService.markAsReadForUser(id, user.getId())) {
            auditService.log("REJECT_READ_NOTIFICATION", "Notification", String.valueOf(id),
                    "Notification not owned by authenticated user");
        }
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(Authentication auth) {
        User user = currentUser(auth);
        if (user == null) return "redirect:/login";

        notificationService.markAllAsRead(user.getId());
        return "redirect:/notifications";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Authentication auth) {
        User user = currentUser(auth);
        if (user == null) return "redirect:/login";

        if (!notificationService.deleteForUser(id, user.getId())) {
            auditService.log("REJECT_DELETE_NOTIFICATION", "Notification", String.valueOf(id),
                    "Notification not owned by authenticated user");
        }
        return "redirect:/notifications";
    }
}
