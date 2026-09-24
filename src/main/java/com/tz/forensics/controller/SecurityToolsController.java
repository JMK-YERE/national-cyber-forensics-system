package com.tz.forensics.controller;

import com.tz.forensics.service.AdvancedSecurityService;
import com.tz.forensics.service.VirusTotalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tools")
public class SecurityToolsController {

    private final AdvancedSecurityService service;
    private final VirusTotalService virusTotalService;

    public SecurityToolsController(AdvancedSecurityService service,
                                    VirusTotalService virusTotalService) {
        this.service = service;
        this.virusTotalService = virusTotalService;
    }

    @GetMapping("/security-center")
    public String securityCenter(Model model) {
        model.addAttribute("vtConfigured", virusTotalService.isConfigured());
        return "security-center";
    }

    @PostMapping("/ssl-check")
    public String sslCheck(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("sslResult", service.checkSSL(url));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/dns-lookup")
    public String dnsLookup(@RequestParam String domain, RedirectAttributes ra) {
        ra.addFlashAttribute("dnsResult", service.dnsLookup(domain));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/ip-check")
    public String ipCheck(@RequestParam String ip, RedirectAttributes ra) {
        ra.addFlashAttribute("ipResult", service.checkIPReputation(ip));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/headers-check")
    public String headersCheck(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("headersResult", service.analyzeHeaders(url));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/hash-check")
    public String hashCheck(@RequestParam String hash, RedirectAttributes ra) {
        ra.addFlashAttribute("hashResult", service.analyzeHash(hash));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/password-breach")
    public String passwordBreach(@RequestParam String password, RedirectAttributes ra) {
        ra.addFlashAttribute("pwBreachResult", service.checkPasswordBreach(password));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/password-check")
    public String passwordCheck(@RequestParam String password, RedirectAttributes ra) {
        ra.addFlashAttribute("pwResult", service.checkPasswordStrength(password));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/url-check")
    public String urlCheck(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("urlResult", service.checkUrlReputation(url));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/domain-age")
    public String domainAge(@RequestParam String domain, RedirectAttributes ra) {
        ra.addFlashAttribute("domainResult", service.checkDomainAge(domain));
        return "redirect:/tools/security-center";
    }

    // ===== VIRUSTOTAL ENDPOINTS =====
    @PostMapping("/vt-url")
    public String vtUrl(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("vtUrlResult", virusTotalService.checkUrl(url));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/vt-hash")
    public String vtHash(@RequestParam String hash, RedirectAttributes ra) {
        ra.addFlashAttribute("vtHashResult", virusTotalService.checkFileHash(hash));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/vt-ip")
    public String vtIp(@RequestParam String ip, RedirectAttributes ra) {
        ra.addFlashAttribute("vtIpResult", virusTotalService.checkIP(ip));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/vt-domain")
    public String vtDomain(@RequestParam String domain, RedirectAttributes ra) {
        ra.addFlashAttribute("vtDomainResult", virusTotalService.checkDomain(domain));
        return "redirect:/tools/security-center";
    }
}
