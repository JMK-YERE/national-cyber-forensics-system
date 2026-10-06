package com.tz.forensics.controller.admin;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.name:Cyber Forensics TZ}")
    private String appName;

    @Value("${app.storage.provider:local}")
    private String storageProvider;

    @Value("${server.servlet.session.timeout:30m}")
    private String sessionTimeout;

    public AdminSettingsController(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        User u = userRepository.findByUsername(auth.getName()).orElse(null);
        return u != null && u.isAdmin();
    }

    @GetMapping
    public String settings(Authentication auth, Model model) {
        if (!isAdmin(auth)) return "redirect:/access-denied";
        model.addAttribute("appName", appName);
        model.addAttribute("emailConfigured", emailService.isConfigured());
        model.addAttribute("storageProvider", storageProvider);
        model.addAttribute("sessionTimeout", sessionTimeout);
        return "admin/settings";
    }
}
