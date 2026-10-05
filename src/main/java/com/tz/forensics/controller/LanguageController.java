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

    public LanguageController(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @GetMapping("/lang")
    public String changeLanguage(@RequestParam(defaultValue = "en") String lang,
                                 @RequestParam(required = false) String redirect,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        Locale locale = "sw".equalsIgnoreCase(lang) ? new Locale("sw") : Locale.ENGLISH;
        localeResolver.setLocale(request, response, locale);

        // Keep the user on the page where the language switch was clicked.
        // Never fall back to "/" for an authenticated request: that can make the
        // user appear to have been logged out when the landing route is reached.
        String target = redirect;
        if (target == null || target.isBlank()) {
            target = request.getHeader("Referer");
        }

        String safe = "/";
        if (target != null && !target.isBlank()) {
            try {
                URI uri = URI.create(target);
                if (!uri.isAbsolute() && uri.getHost() == null && target.startsWith("/")) {
                    safe = target;
                }
            } catch (Exception ignored) {
                safe = "/";
            }
        }

        return "redirect:" + safe;
    }
}
