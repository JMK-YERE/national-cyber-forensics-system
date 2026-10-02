package com.tz.forensics.controller.admin;

import com.tz.forensics.entity.Announcement;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.LandingService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/announcements")
public class AdminAnnouncementController {

    private final LandingService service;
    private final UserRepository userRepository;

    public AdminAnnouncementController(LandingService service, UserRepository userRepository) {
        this.service = service;
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
        model.addAttribute("announcements", service.getAllAnnouncements());
        return "admin/announcements";
    }

    @GetMapping("/new")
    public String newForm(Authentication auth, Model model) {
        if (!isAdmin(auth)) return "redirect:/access-denied";
        model.addAttribute("announcement", new Announcement());
        model.addAttribute("editMode", false);
        return "admin/announcement-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication auth, Model model) {
        if (!isAdmin(auth)) return "redirect:/access-denied";
        Announcement a = service.getAnnouncementById(id);
        if (a == null) return "redirect:/admin/announcements";
        model.addAttribute("announcement", a);
        model.addAttribute("editMode", true);
        return "admin/announcement-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Announcement a, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) return "redirect:/access-denied";
        service.saveAnnouncement(a);
        ra.addFlashAttribute("success", "✅ Announcement imehifadhiwa");
        return "redirect:/admin/announcements";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) return "redirect:/access-denied";
        service.deleteAnnouncement(id);
        ra.addFlashAttribute("success", "🗑️ Announcement imefutwa");
        return "redirect:/admin/announcements";
    }
}
