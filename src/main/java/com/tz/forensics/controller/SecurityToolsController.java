package com.tz.forensics.controller;

import com.tz.forensics.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/tools")
public class SecurityToolsController {

    private final AdvancedSecurityService securityService;
    private final SocialMediaSecurityService socialMediaService;

    public SecurityToolsController(AdvancedSecurityService securityService,
                                    SocialMediaSecurityService socialMediaService) {
        this.securityService = securityService;
        this.socialMediaService = socialMediaService;
    }

    @GetMapping("/security-center")
    public String securityCenter(Model model) {
        model.addAttribute("socialPlatforms", socialMediaService.getAllPlatforms());
        return "security-center";
    }

    // ===== SOCIAL MEDIA DIRECT =====
    @PostMapping("/social-direct")
    public String socialDirect(@RequestParam String platform,
                                @RequestParam(required = false) String username,
                                RedirectAttributes ra) {
        ra.addFlashAttribute("directPlatform", platform);
        ra.addFlashAttribute("directUsername", username);
        ra.addFlashAttribute("directInfo", socialMediaService.getPlatformInfo(platform));
        ra.addFlashAttribute("activeTool", "social");
        return "redirect:/tools/security-center";
    }

    // ===== URL REPUTATION =====
    @PostMapping("/url-check")
    public String checkUrl(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("urlResult", securityService.checkUrlReputation(url));
        ra.addFlashAttribute("activeTool", "url");
        return "redirect:/tools/security-center";
    }

    // ===== SSL =====
    @PostMapping("/ssl-check")
    public String checkSSL(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("sslResult", securityService.checkSSL(url));
        ra.addFlashAttribute("activeTool", "ssl");
        return "redirect:/tools/security-center";
    }

    // ===== DNS =====
    @PostMapping("/dns-lookup")
    public String dnsLookup(@RequestParam String domain, RedirectAttributes ra) {
        ra.addFlashAttribute("dnsResult", securityService.dnsLookup(domain));
        ra.addFlashAttribute("activeTool", "dns");
        return "redirect:/tools/security-center";
    }

    // ===== IP =====
    @PostMapping("/ip-check")
    public String checkIP(@RequestParam String ip, RedirectAttributes ra) {
        ra.addFlashAttribute("ipResult", securityService.checkIPReputation(ip));
        ra.addFlashAttribute("activeTool", "ip");
        return "redirect:/tools/security-center";
    }

    // ===== HEADERS =====
    @PostMapping("/headers-check")
    public String checkHeaders(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("headersResult", securityService.analyzeHeaders(url));
        ra.addFlashAttribute("activeTool", "headers");
        return "redirect:/tools/security-center";
    }

    // ===== HASH =====
    @PostMapping("/hash-check")
    public String checkHash(@RequestParam String hash, RedirectAttributes ra) {
        ra.addFlashAttribute("hashResult", securityService.analyzeHash(hash));
        ra.addFlashAttribute("activeTool", "hash");
        return "redirect:/tools/security-center";
    }

    // ===== PASSWORD BREACH =====
    @PostMapping("/password-breach")
    public String checkPasswordBreach(@RequestParam String password, RedirectAttributes ra) {
        ra.addFlashAttribute("pwBreachResult", securityService.checkPasswordBreach(password));
        ra.addFlashAttribute("activeTool", "pwbreach");
        return "redirect:/tools/security-center";
    }

    // ===== PASSWORD STRENGTH =====
    @PostMapping("/password-check")
    public String checkPassword(@RequestParam String password, RedirectAttributes ra) {
        int score = 0;
        List<String> feedback = new ArrayList<>();
        if (password.length() >= 8) score += 20; else feedback.add("❌ Ongeza urefu (8+)");
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;
        if (password.matches(".*[A-Z].*")) score += 10; else feedback.add("❌ Ongeza herufi kubwa");
        if (password.matches(".*[a-z].*")) score += 10; else feedback.add("❌ Ongeza herufi ndogo");
        if (password.matches(".*\\d.*")) score += 10; else feedback.add("❌ Ongeza namba");
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score += 10;
        else feedback.add("❌ Ongeza alama maalum");

        String lower = password.toLowerCase();
        if (lower.contains("password") || lower.contains("123456") || lower.contains("qwerty")) {
            score = Math.max(0, score - 30);
            feedback.add("⚠️ Password maarufu!");
        }

        score = Math.max(0, Math.min(100, score));
        String rating = score >= 80 ? "STRONG" : score >= 60 ? "GOOD" : score >= 40 ? "WEAK" : "VERY WEAK";
        String emoji = score >= 80 ? "🟢" : score >= 60 ? "🟡" : score >= 40 ? "🟠" : "🔴";
        ra.addFlashAttribute("pwScore", score);
        ra.addFlashAttribute("pwRating", rating);
        ra.addFlashAttribute("pwEmoji", emoji);
        ra.addFlashAttribute("pwFeedback", feedback);
        ra.addFlashAttribute("activeTool", "password");
        return "redirect:/tools/security-center";
    }

    // ===== DOMAIN AGE =====
    @PostMapping("/domain-age")
    public String checkDomainAge(@RequestParam String domain, RedirectAttributes ra) {
        ra.addFlashAttribute("domainResult", securityService.checkDomainAge(domain));
        ra.addFlashAttribute("activeTool", "domain");
        return "redirect:/tools/security-center";
    }

    @GetMapping("/tips")
    public String securityTips(Model model) {
        List<Map<String, String>> tips = new ArrayList<>();
        tips.add(createTip("🔐", "Password Nzuri", "Herufi 12+, kubwa/ndogo, namba, alama (!@#$%)"));
        tips.add(createTip("📱", "Weka 2FA", "Facebook, IG, WhatsApp, Gmail, TikTok — kila mahali"));
        tips.add(createTip("🎣", "Jihadharini Phishing", "Usibonyeze links za kutiliwa shaka. Angalia URL vizuri."));
        tips.add(createTip("🔒", "Sasisha Programs", "OS, browsers, apps — sasisha kila mara"));
        tips.add(createTip("📶", "Tumia HTTPS", "Angalia 🔒 kwenye browser kila unapoingiza password"));
        tips.add(createTip("💾", "Backup Data", "Backup data muhimu kila wiki"));
        tips.add(createTip("📧", "Angalia Breaches", "Check HaveIBeenPwned kila miezi 3"));
        tips.add(createTip("🚫", "Epuka WiFi ya Public", "Usiingize password kwenye WiFi ya hoteli"));
        tips.add(createTip("👁️", "Angalia Login Activity", "Check accounts kila wiki"));
        tips.add(createTip("🎓", "Elimu ya Usalama", "Jifunze kuhusu cyber security kila siku"));
        model.addAttribute("tips", tips);
        return "security-tips";
    }

    private Map<String, String> createTip(String icon, String title, String body) {
        Map<String, String> tip = new HashMap<>();
        tip.put("icon", icon); tip.put("title", title); tip.put("body", body);
        return tip;
    }
}
