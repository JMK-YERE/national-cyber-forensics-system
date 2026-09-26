package com.tz.forensics.controller.admin;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/audit")
public class AdminAuditController {

    private final UserRepository userRepository;

    public AdminAuditController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        User u = userRepository.findByUsername(auth.getName()).orElse(null);
        return u != null && (u.isAdmin() || u.isProfessional() || u.isForensics());
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        if (!isAdmin(auth)) return "redirect:/access-denied";
        model.addAttribute("message", "Audit Log — kumbukumbu za vitendo vyote");
        return "admin/audit";
    }
}
