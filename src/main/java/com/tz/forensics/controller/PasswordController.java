package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/forgot-password")
    public String forgotForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotSubmit(@RequestParam String email, RedirectAttributes ra) {
        // TODO: Implement actual email sending
        ra.addFlashAttribute("sent", true);
        return "redirect:/forgot-password?sent";
    }

    @GetMapping("/change-password")
    public String changeForm() {
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changeSubmit(@RequestParam String currentPassword,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Authentication auth,
                                RedirectAttributes ra) {
        if (auth == null) return "redirect:/login";

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "Neno la siri la sasa si sahihi");
            return "redirect:/change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Maneno ya siri hayafanani");
            return "redirect:/change-password";
        }

        if (newPassword.length() < 6) {
            ra.addFlashAttribute("error", "Neno la siri liwe herufi 6+");
            return "redirect:/change-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        ra.addFlashAttribute("success", "✅ Neno la siri limebadilishwa!");
        return "redirect:/change-password";
    }
}
