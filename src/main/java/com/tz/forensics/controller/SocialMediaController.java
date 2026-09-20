package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.SocialMediaMonitorService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/my-accounts")
public class SocialMediaController {

    private final SocialMediaMonitorService monitorService;
    private final UserRepository userRepository;

    public SocialMediaController(SocialMediaMonitorService monitorService,
                                   UserRepository userRepository) {
        this.monitorService = monitorService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String showForm(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "social-media-monitor";
    }

    @PostMapping("/check")
    public String checkAccounts(@RequestParam(required = false) String facebook,
                                 @RequestParam(required = false) String instagram,
                                 @RequestParam(required = false) String tiktok,
                                 @RequestParam(required = false) String whatsapp,
                                 @RequestParam(required = false) String x,
                                 @RequestParam(required = false) String gmail,
                                 RedirectAttributes ra) {

        List<Map<String, Object>> results = new ArrayList<>();

        if (facebook != null && !facebook.isBlank())
            results.add(monitorService.checkFacebook(facebook.trim()));
        if (instagram != null && !instagram.isBlank())
            results.add(monitorService.checkInstagram(instagram.trim()));
        if (tiktok != null && !tiktok.isBlank())
            results.add(monitorService.checkTikTok(tiktok.trim()));
        if (whatsapp != null && !whatsapp.isBlank())
            results.add(monitorService.checkWhatsApp(whatsapp.trim()));
        if (x != null && !x.isBlank())
            results.add(monitorService.checkX(x.trim()));
        if (gmail != null && !gmail.isBlank())
            results.add(monitorService.checkGmail(gmail.trim()));

        // Calculate score
        int score = 0;
        if (!results.isEmpty()) {
            score = 30; // Base
            score += Math.min(40, results.size() * 10); // 10 kwa kila account
            long existsCount = results.stream().filter(r -> Boolean.TRUE.equals(r.get("exists"))).count();
            score += (int) (existsCount * 5); // Bonus kwa kila account inayopatikana
            if (score > 100) score = 100;
        }

        ra.addFlashAttribute("accounts", results);
        ra.addFlashAttribute("score", score);
        ra.addFlashAttribute("totalAccounts", results.size());

        return "redirect:/my-accounts";
    }
}
