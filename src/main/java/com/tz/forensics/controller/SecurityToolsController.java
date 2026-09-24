package com.tz.forensics.controller;

import com.tz.forensics.service.AdvancedSecurityService;
import com.tz.forensics.service.URLScanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tools")
public class SecurityToolsController {

    private final AdvancedSecurityService service;
    private final URLScanService urlScanService;

    public SecurityToolsController(AdvancedSecurityService service,
                                    URLScanService urlScanService) {
        this.service = service;
        this.urlScanService = urlScanService;
    }

    @GetMapping("/security-center")
    public String securityCenter(Model model) {
        model.addAttribute("urlscanConfigured", urlScanService.isConfigured());
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

    // ===== URLSCAN =====
    @PostMapping("/urlscan-scan")
    public String urlscanScan(@RequestParam String url, RedirectAttributes ra) {
        ra.addFlashAttribute("urlscanResult", urlScanService.scanUrl(url));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/urlscan-result")
    public String urlscanResult(@RequestParam String uuid, RedirectAttributes ra) {
        ra.addFlashAttribute("urlscanDetail", urlScanService.getResult(uuid));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/urlscan-search")
    public String urlscanSearch(@RequestParam String domain, RedirectAttributes ra) {
        ra.addFlashAttribute("urlscanSearch", urlScanService.searchDomain(domain));
        return "redirect:/tools/security-center";
    }
}
