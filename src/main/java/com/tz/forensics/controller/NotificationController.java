package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
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

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listNotifications(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        model.addAttribute("notifications", notificationService.getNotificationsForUser(user.getId()));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user.getId()));
        return "notifications";
    }

    @PostMapping("/read/{id}")
    public String markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user != null) {
            notificationService.markAllAsRead(user.getId());
        }
        return "redirect:/notifications";
    }
}
