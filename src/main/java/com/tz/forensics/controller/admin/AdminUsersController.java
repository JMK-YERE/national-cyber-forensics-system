package com.tz.forensics.controller.admin;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class AdminUsersController {

    private final UserRepository userRepository;

    public AdminUsersController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        User u = userRepository.findByUsername(auth.getName()).orElse(null);
        return u != null && u.isAdmin();
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        if (!isAdmin(auth)) return "redirect:/access-denied";

        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("adminCount", users.stream().filter(User::isAdmin).count());
        model.addAttribute("professionalCount", users.stream().filter(User::isProfessional).count());
        model.addAttribute("individualCount", users.stream().filter(u -> "INDIVIDUAL".equalsIgnoreCase(u.getRole())).count());
        return "admin/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) return "redirect:/access-denied";
        User u = userRepository.findById(id).orElse(null);
        if (u != null) {
            u.setEnabled(!Boolean.TRUE.equals(u.getEnabled()));
            userRepository.save(u);
            ra.addFlashAttribute("success", "User " + u.getUsername() + " imebadilishwa");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id,
                              @org.springframework.web.bind.annotation.RequestParam String role,
                              Authentication auth,
                              RedirectAttributes ra) {
        if (!isAdmin(auth)) return "redirect:/access-denied";
        User u = userRepository.findById(id).orElse(null);
        if (u != null) {
            u.setRole(role);
            userRepository.save(u);
            ra.addFlashAttribute("success", "Role ya " + u.getUsername() + " imebadilishwa kuwa " + role);
        }
        return "redirect:/admin/users";
    }
}
