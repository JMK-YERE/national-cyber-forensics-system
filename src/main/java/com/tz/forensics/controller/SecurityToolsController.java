package com.tz.forensics.controller;

import com.tz.forensics.service.AdvancedSecurityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/tools")
public class SecurityToolsController {

    private final AdvancedSecurityService service;

    public SecurityToolsController(AdvancedSecurityService service) {
        this.service = service;
    }

    @GetMapping("/security-center")
    public String securityCenter() {
        return "security-center";
    }

    // ===== 1. SSL =====
    @PostMapping("/ssl-check")
    public String sslCheck(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("sslResult", service.checkSSL(url));
        ra.addFlashAttribute("activeTool", "ssl");
        return "redirect:/tools/security-center";
    }

    // ===== 2. DNS =====
    @PostMapping("/dns-lookup")
    public String dnsLookup(@RequestParam String domain, RedirectAttributes ra) {
        ra.addFlashAttribute("dnsResult", service.dnsLookup(domain));
        ra.addFlashAttribute("activeTool", "dns");
        return "redirect:/tools/security-center";
    }

    // ===== 3. IP REPUTATION =====
    @PostMapping("/ip-check")
    public String ipCheck(@RequestParam String ip, RedirectAttributes ra) {
        ra.addFlashAttribute("ipResult", service.checkIPReputation(ip));
        ra.addFlashAttribute("activeTool", "ip");
        return "redirect:/tools/security-center";
    }

    // ===== 4. HEADERS =====
    @PostMapping("/headers-check")
    public String headersCheck(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("headersResult", service.analyzeHeaders(url));
        ra.addFlashAttribute("activeTool", "headers");
        return "redirect:/tools/security-center";
    }

    // ===== 5. HASH =====
    @PostMapping("/hash-check")
    public String hashCheck(@RequestParam String hash, RedirectAttributes ra) {
        ra.addFlashAttribute("hashResult", service.analyzeHash(hash));
        ra.addFlashAttribute("activeTool", "hash");
        return "redirect:/tools/security-center";
    }

    // ===== 6. PASSWORD BREACH =====
    @PostMapping("/password-breach")
    public String passwordBreach(@RequestParam String password, RedirectAttributes ra) {
        ra.addFlashAttribute("pwBreachResult", service.checkPasswordBreach(password));
        ra.addFlashAttribute("activeTool", "pwbreach");
        return "redirect:/tools/security-center";
    }

    // ===== 7. PASSWORD STRENGTH =====
    @PostMapping("/password-check")
    public String passwordCheck(@RequestParam String password, RedirectAttributes ra) {
        ra.addFlashAttribute("pwResult", service.checkPasswordStrength(password));
        ra.addFlashAttribute("activeTool", "pwstrength");
        return "redirect:/tools/security-center";
    }

    // ===== 8. URL REPUTATION =====
    @PostMapping("/url-check")
    public String urlCheck(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("urlResult", service.checkUrlReputation(url));
        ra.addFlashAttribute("activeTool", "url");
        return "redirect:/tools/security-center";
    }

    // ===== 9. DOMAIN AGE =====
    @PostMapping("/domain-age")
    public String domainAge(@RequestParam String domain, RedirectAttributes ra) {
        ra.addFlashAttribute("domainResult", service.checkDomainAge(domain));
        ra.addFlashAttribute("activeTool", "domain");
        return "redirect:/tools/security-center";
    }
}
