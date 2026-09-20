package com.tz.forensics.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Controller
@RequestMapping("/tools")
public class SecurityToolsController {

    // ===== SECURITY CENTER DASHBOARD =====
    @GetMapping("/security-center")
    public String securityCenter() {
        return "security-center";
    }

    // ===== PASSWORD STRENGTH ANALYZER =====
    @PostMapping("/password-check")
    public String checkPassword(@RequestParam String password,
                                RedirectAttributes redirectAttributes) {
        int score = 0;
        List<String> feedback = new ArrayList<>();

        // Length
        if (password.length() >= 8) score += 20;
        else feedback.add("❌ Ongeza urefu (angalau herufi 8)");
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;

        // Uppercase
        if (password.matches(".*[A-Z].*")) score += 10;
        else feedback.add("❌ Ongeza herufi kubwa (A-Z)");

        // Lowercase
        if (password.matches(".*[a-z].*")) score += 10;
        else feedback.add("❌ Ongeza herufi ndogo (a-z)");

        // Numbers
        if (password.matches(".*\\d.*")) score += 10;
        else feedback.add("❌ Ongeza namba (0-9)");

        // Special characters
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score += 10;
        else feedback.add("❌ Ongeza alama maalum (!@#$%^&*)");

        // Common patterns
        String lower = password.toLowerCase();
        if (lower.contains("password") || lower.contains("123456") ||
            lower.contains("qwerty") || lower.contains("admin")) {
            score = Math.max(0, score - 30);
            feedback.add("⚠️ Password hii ni maarufu — badilisha!");
        }

        // Date patterns
        if (password.matches(".*(19|20)\\d{2}.*")) {
            score -= 10;
            feedback.add("⚠️ Usitumie mwaka wa kuzaliwa");
        }

        score = Math.max(0, Math.min(100, score));

        String rating = score >= 80 ? "STRONG" : score >= 60 ? "GOOD" :
                        score >= 40 ? "WEAK" : "VERY WEAK";
        String emoji = score >= 80 ? "🟢" : score >= 60 ? "🟡" :
                       score >= 40 ? "🟠" : "🔴";

        redirectAttributes.addFlashAttribute("pwScore", score);
        redirectAttributes.addFlashAttribute("pwRating", rating);
        redirectAttributes.addFlashAttribute("pwEmoji", emoji);
        redirectAttributes.addFlashAttribute("pwFeedback", feedback);
        return "redirect:/tools/security-center";
    }

    // ===== EMAIL BREACH CHECKER (HaveIBeenPwned - BURE) =====
    @PostMapping("/email-check")
    public String checkEmail(@RequestParam String email,
                             RedirectAttributes redirectAttributes) {
        try {
            String url = "https://haveibeenpwned.com/api/v3/breachedaccount/" +
                    email + "?truncateResponse=false";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "CyberForensicsTZ")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                redirectAttributes.addFlashAttribute("breachEmail", email);
                redirectAttributes.addFlashAttribute("breachStatus", "BREACHED");
                redirectAttributes.addFlashAttribute("breachData", response.body());
                redirectAttributes.addFlashAttribute("breachCount", countOccurrences(response.body(), "\"Name\":"));
            } else if (response.statusCode() == 404) {
                redirectAttributes.addFlashAttribute("breachEmail", email);
                redirectAttributes.addFlashAttribute("breachStatus", "SAFE");
            } else {
                redirectAttributes.addFlashAttribute("breachEmail", email);
                redirectAttributes.addFlashAttribute("breachStatus", "UNKNOWN");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("breachEmail", email);
            redirectAttributes.addFlashAttribute("breachStatus", "ERROR");
            redirectAttributes.addFlashAttribute("breachError", e.getMessage());
        }
        return "redirect:/tools/security-center";
    }

    // ===== PHISHING URL CHECKER =====
    @PostMapping("/url-check")
    public String checkUrl(@RequestParam String url,
                           RedirectAttributes redirectAttributes) {
        List<String> warnings = new ArrayList<>();
        int riskScore = 0;

        String lower = url.toLowerCase();

        // HTTP (not HTTPS)
        if (lower.startsWith("http://")) {
            riskScore += 30;
            warnings.add("🔴 Haijatumia HTTPS — sio salama");
        }

        // Suspicious TLDs
        String[] suspiciousTlds = {".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top", ".work", ".click"};
        for (String tld : suspiciousTlds) {
            if (lower.contains(tld)) {
                riskScore += 25;
                warnings.add("🔴 TLD inayotiliwa shaka: " + tld);
                break;
            }
        }

        // IP address in URL
        if (lower.matches(".*://\\d+\\.\\d+\\.\\d+\\.\\d+.*")) {
            riskScore += 40;
            warnings.add("🔴 URL ina IP address badala ya domain");
        }

        // Too many subdomains
        if (lower.split("\\.").length > 5) {
            riskScore += 20;
            warnings.add("🟠 Subdomains nyingi sana");
        }

        // Long URL
        if (url.length() > 100) {
            riskScore += 15;
            warnings.add("🟠 URL ni ndefu sana");
        }

        // Suspicious keywords
        String[] keywords = {"login", "verify", "account", "update", "secure", "bank", "paypal", "signin"};
        for (String kw : keywords) {
            if (lower.contains(kw) && !lower.contains("google.com") && !lower.contains("microsoft.com")) {
                riskScore += 10;
                warnings.add("🟠 Ina neno la kutiliwa shaka: " + kw);
            }
        }

        // @ symbol in URL
        if (lower.contains("@") && !lower.startsWith("mailto:")) {
            riskScore += 30;
            warnings.add("🔴 Ina alama @ — inaweza kuwa redirect");
        }

        riskScore = Math.min(100, riskScore);

        String verdict;
        String emoji;
        if (riskScore >= 60) { verdict = "PHISHING"; emoji = "🚨"; }
        else if (riskScore >= 30) { verdict = "SUSPICIOUS"; emoji = "⚠️"; }
        else { verdict = "SAFE"; emoji = "✅"; warnings.add("✅ URL inaonekana salama"); }

        redirectAttributes.addFlashAttribute("urlChecked", url);
        redirectAttributes.addFlashAttribute("urlRisk", riskScore);
        redirectAttributes.addFlashAttribute("urlVerdict", verdict);
        redirectAttributes.addFlashAttribute("urlEmoji", emoji);
        redirectAttributes.addFlashAttribute("urlWarnings", warnings);
        return "redirect:/tools/security-center";
    }

    // ===== SECURITY TIPS =====
    @GetMapping("/tips")
    public String securityTips(Model model) {
        List<Map<String, String>> tips = new ArrayList<>();

        tips.add(createTip("🔐", "Tumia Password Nzuri",
            "Tumia password yenye herufi 12+, herufi kubwa na ndogo, namba, na alama. Usitumie password ile ile kwenye sites zote."));
        tips.add(createTip("📱", "Weka 2FA",
            "Weka Two-Factor Authentication kwenye Facebook, Instagram, WhatsApp, Gmail, na TikTok. Hii inazuia hacking hata kama mtu anajua password yako."));
        tips.add(createTip("🎣", "Jihadharini na Phishing",
            "Usibonyeze links kwenye email/SMS za kutiliwa shaka. Angalia URL vizuri — hasa zile zinazoanza na http:// badala ya https://."));
        tips.add(createTip("🔒", "Sasisha Programs",
            "Sasisha OS, browsers, na apps zako kila mara. Updates zina fix security flaws."));
        tips.add(createTip("📶", "Tumia HTTPS",
            "Angalia alama ya 🔒 kwenye browser. Usiingize password kwenye sites zinazoanza http://."));
        tips.add(createTip("💾", "Backup Data Yako",
            "Fanya backup ya data muhimu kila wiki. Hii inakusaidia kama unapata ransomware."));
        tips.add(createTip("📧", "Angalia Email Zako",
            "Check HaveIBeenPwned kila miezi 3 kuona kama email yako imevuja."));
        tips.add(createTip("🚫", "Usitumie WiFi ya Public",
            "Epuka kuingiza password kwenye WiFi ya hoteli, mgahawa, au airport. Tumia VPN."));
        tips.add(createTip("👁️", "Angalia Login Activity",
            "Check activity ya accounts zako kila wiki. Kama unaona login usiyoijua, badilisha password mara moja."));
        tips.add(createTip("🎓", "Elimu ya Usalama",
            "Jifunze kuhusu cyber security. Watu wengi wanahack kwa sababu hawajui hatari."));

        model.addAttribute("tips", tips);
        return "security-tips";
    }

    private Map<String, String> createTip(String icon, String title, String body) {
        Map<String, String> tip = new HashMap<>();
        tip.put("icon", icon);
        tip.put("title", title);
        tip.put("body", body);
        return tip;
    }

    private int countOccurrences(String str, String sub) {
        int count = 0, idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) { count++; idx += sub.length(); }
        return count;
    }
}
