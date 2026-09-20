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
    private final VirusTotalService virusTotalService;
    private final URLScanService urlScanService;
    private final IPGeolocationService ipGeoService;

    public SecurityToolsController(AdvancedSecurityService securityService,
                                    VirusTotalService virusTotalService,
                                    URLScanService urlScanService,
                                    IPGeolocationService ipGeoService) {
        this.securityService = securityService;
        this.virusTotalService = virusTotalService;
        this.urlScanService = urlScanService;
        this.ipGeoService = ipGeoService;
    }

    @GetMapping("/security-center")
    public String securityCenter(Model model) {
        model.addAttribute("vtConfigured", virusTotalService.isConfigured());
        return "security-center";
    }

    // ===== VIRUSTOTAL URL =====
    @PostMapping("/virustotal-url")
    public String vtUrl(@RequestParam String url, RedirectAttributes ra) {
        String json = virusTotalService.checkUrl(url);
        int malicious = virusTotalService.parseMalicious(json);
        int suspicious = virusTotalService.parseSuspicious(json);
        int harmless = virusTotalService.parseHarmless(json);
        ra.addFlashAttribute("vtUrlResult", url);
        ra.addFlashAttribute("vtMalicious", malicious);
        ra.addFlashAttribute("vtSuspicious", suspicious);
        ra.addFlashAttribute("vtHarmless", harmless);
        ra.addFlashAttribute("activeTool", "vturl");
        return "redirect:/tools/security-center";
    }

    // ===== VIRUSTOTAL HASH =====
    @PostMapping("/virustotal-hash")
    public String vtHash(@RequestParam String hash, RedirectAttributes ra) {
        String json = virusTotalService.checkFileHash(hash);
        ra.addFlashAttribute("vtHashResult", hash);
        ra.addFlashAttribute("vtHashMalicious", virusTotalService.parseMalicious(json));
        ra.addFlashAttribute("vtHashSuspicious", virusTotalService.parseSuspicious(json));
        ra.addFlashAttribute("activeTool", "vthash");
        return "redirect:/tools/security-center";
    }

    // ===== VIRUSTOTAL IP =====
    @PostMapping("/virustotal-ip")
    public String vtIp(@RequestParam String ip, RedirectAttributes ra) {
        String json = virusTotalService.checkIP(ip);
        ra.addFlashAttribute("vtIpResult", ip);
        ra.addFlashAttribute("vtIpMalicious", virusTotalService.parseMalicious(json));
        ra.addFlashAttribute("activeTool", "vtip");
        return "redirect:/tools/security-center";
    }

    // ===== VIRUSTOTAL DOMAIN =====
    @PostMapping("/virustotal-domain")
    public String vtDomain(@RequestParam String domain, RedirectAttributes ra) {
        String json = virusTotalService.checkDomain(domain);
        ra.addFlashAttribute("vtDomainResult", domain);
        ra.addFlashAttribute("vtDomainMalicious", virusTotalService.parseMalicious(json));
        ra.addFlashAttribute("activeTool", "vtdomain");
        return "redirect:/tools/security-center";
    }

    // ===== IP GEOLOCATION =====
    @PostMapping("/ip-geo")
    public String ipGeo(@RequestParam String ip, RedirectAttributes ra) {
        ra.addFlashAttribute("geoResult", ipGeoService.lookup(ip));
        ra.addFlashAttribute("geoIp", ip);
        ra.addFlashAttribute("activeTool", "geo");
        return "redirect:/tools/security-center";
    }

    // ===== URL REPUTATION =====
    @PostMapping("/url-check")
    public String checkUrl(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("urlResult", securityService.checkUrlReputation(url));
        ra.addFlashAttribute("activeTool", "url");
        return "redirect:/tools/security-center";
    }

    @PostMapping("/ssl-check")
    public String checkSSL(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("sslResult", securityService.checkSSL(url));
        ra.addFlashAttribute("activeTool", "ssl");
        return "redirect:/tools/security-center";
    }

    @PostMapping("/dns-lookup")
    public String dnsLookup(@RequestParam String domain, RedirectAttributes ra) {
        ra.addFlashAttribute("dnsResult", securityService.dnsLookup(domain));
        ra.addFlashAttribute("activeTool", "dns");
        return "redirect:/tools/security-center";
    }

    @PostMapping("/ip-check")
    public String checkIP(@RequestParam String ip, RedirectAttributes ra) {
        ra.addFlashAttribute("ipResult", securityService.checkIPReputation(ip));
        ra.addFlashAttribute("activeTool", "ip");
        return "redirect:/tools/security-center";
    }

    @PostMapping("/headers-check")
    public String checkHeaders(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("headersResult", securityService.analyzeHeaders(url));
        ra.addFlashAttribute("activeTool", "headers");
        return "redirect:/tools/security-center";
    }

    @PostMapping("/hash-check")
    public String checkHash(@RequestParam String hash, RedirectAttributes ra) {
        ra.addFlashAttribute("hashResult", securityService.analyzeHash(hash));
        ra.addFlashAttribute("activeTool", "hash");
        return "redirect:/tools/security-center";
    }

    @PostMapping("/password-breach")
    public String checkPasswordBreach(@RequestParam String password, RedirectAttributes ra) {
        ra.addFlashAttribute("pwBreachResult", securityService.checkPasswordBreach(password));
        ra.addFlashAttribute("activeTool", "pwbreach");
        return "redirect:/tools/security-center";
    }

    @PostMapping("/domain-age")
    public String checkDomainAge(@RequestParam String domain, RedirectAttributes ra) {
        ra.addFlashAttribute("domainResult", securityService.checkDomainAge(domain));
        ra.addFlashAttribute("activeTool", "domain");
        return "redirect:/tools/security-center";
    }

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

    @GetMapping("/tips")
    public String securityTips(Model model) {
        List<Map<String, String>> tips = new ArrayList<>();
        tips.add(createTip("🔐", "Password Nzuri", "Herufi 12+, kubwa/ndogo, namba, alama"));
        tips.add(createTip("📱", "Weka 2FA", "Facebook, IG, WhatsApp, Gmail, TikTok"));
        tips.add(createTip("🎣", "Jihadharini Phishing", "Usibonyeze links za kutiliwa shaka"));
        tips.add(createTip("🔒", "Sasisha Programs", "OS, browsers, apps kila mara"));
        tips.add(createTip("📶", "Tumia HTTPS", "Angalia 🔒 kwenye browser"));
        tips.add(createTip("💾", "Backup Data", "Backup data muhimu kila wiki"));
        tips.add(createTip("📧", "Angalia Breaches", "Check HaveIBeenPwned kila miezi 3"));
        tips.add(createTip("🚫", "Epuka WiFi ya Public", "Usiingize password kwenye WiFi ya hoteli"));
        tips.add(createTip("👁️", "Angalia Login Activity", "Check accounts kila wiki"));
        tips.add(createTip("🎓", "Elimu ya Usalama", "Jifunze kuhusu cyber security"));
        model.addAttribute("tips", tips);
        return "security-tips";
    }

    private Map<String, String> createTip(String icon, String title, String body) {
        Map<String, String> tip = new HashMap<>();
        tip.put("icon", icon); tip.put("title", title); tip.put("body", body);
        return tip;
    }
}
