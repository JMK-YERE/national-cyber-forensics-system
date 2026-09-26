package com.tz.forensics.controller.admin;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final UserRepository userRepository;

    public AdminSettingsController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        User u = userRepository.findByUsername(auth.getName()).orElse(null);
        return u != null && u.isAdmin();
    }

    @GetMapping
    public String settings(Authentication auth, Model model) {
        if (!isAdmin(auth)) return "redirect:/access-denied";
        return "admin/settings";
    }
}
