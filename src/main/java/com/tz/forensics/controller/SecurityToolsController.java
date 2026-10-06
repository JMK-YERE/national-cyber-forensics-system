package com.tz.forensics.controller;

import com.tz.forensics.service.AdvancedSecurityService;
import com.tz.forensics.service.VirusTotalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    private boolean staff(Authentication auth) {
        return authenticated(auth) && auth.getAuthorities().stream().anyMatch(a ->
                java.util.Set.of("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST")
                        .contains(a.getAuthority()));
    }

    private boolean authenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName());
    }

    @GetMapping("/security-center")
    public String securityCenter(Authentication auth, Model model) {
        if (!authenticated(auth)) return "redirect:/login";
        model.addAttribute("vtConfigured", virusTotalService.isConfigured());
        boolean staff = auth.getAuthorities().stream().anyMatch(a ->
                java.util.Set.of("ROLE_ADMIN", "ROLE_CYBER_PRO", "ROLE_FORENSICS", "ROLE_ANALYST").contains(a.getAuthority()));
        model.addAttribute("isStaff", staff);
        return "security-center";
    }

    @PostMapping("/ssl-check")
    public String sslCheck(@RequestParam String url, Authentication auth, RedirectAttributes ra) {
        if (!staff(auth)) return "redirect:/access-denied";
        ra.addFlashAttribute("sslResult", service.checkSSL(url));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/dns-lookup")
    public String dnsLookup(@RequestParam String domain, Authentication auth, RedirectAttributes ra) {
        if (!staff(auth)) return "redirect:/access-denied";
        ra.addFlashAttribute("dnsResult", service.dnsLookup(domain));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/ip-check")
    public String ipCheck(@RequestParam String ip, Authentication auth, RedirectAttributes ra) {
        if (!staff(auth)) return "redirect:/access-denied";
        ra.addFlashAttribute("ipResult", service.checkIPReputation(ip));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/headers-check")
    public String headersCheck(@RequestParam String url, Authentication auth, RedirectAttributes ra) {
        if (!staff(auth)) return "redirect:/access-denied";
        ra.addFlashAttribute("headersResult", service.analyzeHeaders(url));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/hash-check")
    public String hashCheck(@RequestParam String hash, Authentication auth, RedirectAttributes ra) {
        if (!authenticated(auth)) return "redirect:/login";
        ra.addFlashAttribute("hashResult", service.analyzeHash(hash));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/password-breach")
    public String passwordBreach(@RequestParam String password, Authentication auth, RedirectAttributes ra) {
        if (!staff(auth)) return "redirect:/access-denied";
        ra.addFlashAttribute("pwBreachResult", service.checkPasswordBreach(password));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/password-check")
    public String passwordCheck(@RequestParam String password, Authentication auth, RedirectAttributes ra) {
        if (!authenticated(auth)) return "redirect:/login";
        ra.addFlashAttribute("pwResult", service.checkPasswordStrength(password));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/url-check")
    public String urlCheck(@RequestParam String url, Authentication auth, RedirectAttributes ra) {
        if (!authenticated(auth)) return "redirect:/login";
        ra.addFlashAttribute("urlResult", service.checkUrlReputation(url));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/url-check-api")
    @ResponseBody
    public ResponseEntity<Object> urlCheckApi(@RequestParam String url, Authentication auth) {
        if (!authenticated(auth)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(service.checkUrlReputation(url));
    }

    @PostMapping("/domain-age")
    public String domainAge(@RequestParam String domain, Authentication auth, RedirectAttributes ra) {
        if (!staff(auth)) return "redirect:/access-denied";
        ra.addFlashAttribute("domainResult", service.checkDomainAge(domain));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/vt-url")
    public String vtUrl(@RequestParam String url, Authentication auth, RedirectAttributes ra) {
        if (!staff(auth)) return "redirect:/access-denied";
        ra.addFlashAttribute("vtUrlResult", virusTotalService.checkUrl(url));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/vt-hash")
    public String vtHash(@RequestParam String hash, Authentication auth, RedirectAttributes ra) {
        if (!staff(auth)) return "redirect:/access-denied";
        ra.addFlashAttribute("vtHashResult", virusTotalService.checkFileHash(hash));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/vt-ip")
    public String vtIp(@RequestParam String ip, Authentication auth, RedirectAttributes ra) {
        if (!staff(auth)) return "redirect:/access-denied";
        ra.addFlashAttribute("vtIpResult", virusTotalService.checkIP(ip));
        return "redirect:/tools/security-center";
    }

    @PostMapping("/vt-domain")
    public String vtDomain(@RequestParam String domain, Authentication auth, RedirectAttributes ra) {
        if (!staff(auth)) return "redirect:/access-denied";
        ra.addFlashAttribute("vtDomainResult", virusTotalService.checkDomain(domain));
        return "redirect:/tools/security-center";
    }
}