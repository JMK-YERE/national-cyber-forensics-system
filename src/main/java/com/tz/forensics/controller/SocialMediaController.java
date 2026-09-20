package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AdvancedSecurityService;
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
    private final AdvancedSecurityService securityService;
    private final UserRepository userRepository;

    public SocialMediaController(SocialMediaMonitorService monitorService,
                                   AdvancedSecurityService securityService,
                                   UserRepository userRepository) {
        this.monitorService = monitorService;
        this.securityService = securityService;
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
                                 Authentication auth,
                                 RedirectAttributes ra) {

        List<Map<String, Object>> results = new ArrayList<>();

        if (facebook != null && !facebook.isBlank())
            results.add(monitorService.checkFacebook(facebook.trim()));
        if (instagram != null && !instagram.isBlank())
            results.add(monitorService.checkInstagram(instagram.trim()));
        if (tiktok != null && !tiktok.isBlank())
            results.add(monitorService.checkTiktok(tiktok.trim()));
        if (whatsapp != null && !whatsapp.isBlank())
            results.add(monitorService.checkWhatsApp(whatsapp.trim()));
        if (x != null && !x.isBlank())
            results.add(monitorService.checkX(x.trim()));
        if (gmail != null && !gmail.isBlank())
            results.add(monitorService.checkGmail(gmail.trim()));

        // Calculate score
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        boolean has2fa = user != null && Boolean.TRUE.equals(user.getHas2fa());
        boolean breachChecked = user != null && Boolean.TRUE.equals(user.getBreachChecked());
        int score = monitorService.calculateScore(results, has2fa, breachChecked);

        ra.addFlashAttribute("accounts", results);
        ra.addFlashAttribute("score", score);
        ra.addFlashAttribute("totalAccounts", results.size());

        return "redirect:/my-accounts";
    }

    // ===== Check Gmail breach =====
    @PostMapping("/breach-check")
    public String breachCheck(@RequestParam String email, RedirectAttributes ra) {
        try {
            String url = "https://haveibeenpwned.com/api/v3/breachedaccount/" + email;
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("User-Agent", "CyberForensicsTZ")
                    .GET().build();
            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            ra.addFlashAttribute("breachEmail", email);
            ra.addFlashAttribute("breachStatus", response.statusCode() == 200 ? "BREACHED" : "SAFE");
        } catch (Exception e) {
            ra.addFlashAttribute("breachEmail", email);
            ra.addFlashAttribute("breachStatus", "ERROR");
        }
        return "redirect:/my-accounts";
    }
}
