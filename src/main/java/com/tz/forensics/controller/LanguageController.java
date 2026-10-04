package com.tz.forensics.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import java.net.URI;
import java.util.Locale;

@Controller
public class LanguageController {
    private final LocaleResolver localeResolver;
    public LanguageController(LocaleResolver localeResolver) { this.localeResolver = localeResolver; }

    @GetMapping("/lang")
    public String changeLanguage(@RequestParam(defaultValue = "en") String lang,
                                 @RequestParam(defaultValue = "/") String redirect,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        Locale locale = "sw".equalsIgnoreCase(lang) ? new Locale("sw") : Locale.ENGLISH;
        localeResolver.setLocale(request, response, locale);
        String safe = redirect == null || redirect.isBlank() ? "/" : redirect;
        try {
            URI uri = URI.create(safe);
            if (uri.isAbsolute() || uri.getHost() != null || !safe.startsWith("/")) safe = "/";
        } catch (Exception e) { safe = "/"; }
        return "redirect:" + safe;
    }
}