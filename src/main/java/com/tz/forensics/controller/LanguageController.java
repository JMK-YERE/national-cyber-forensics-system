package com.tz.forensics.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LanguageController {

    @GetMapping("/lang")
    public String switchLanguage(@RequestParam String lang,
                                  @RequestParam(required = false) String redirect,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        // Set cookie for language
        Cookie cookie = new Cookie("lang", lang);
        cookie.setMaxAge(365 * 24 * 60 * 60);
        cookie.setPath("/");
        response.addCookie(cookie);

        // Redirect logic
        String target = "/dashboard";

        if (redirect != null && !redirect.isEmpty()) {
            target = redirect;
        } else {
            // Use Referer header
            String referer = request.getHeader("Referer");
            if (referer != null && referer.contains("/")) {
                try {
                    String path = referer.substring(referer.indexOf("/", 8));
                    if (path.startsWith("/") && !path.contains("/lang")) {
                        // Remove query params
                        if (path.contains("?")) {
                            path = path.substring(0, path.indexOf("?"));
                        }
                        target = path;
                    }
                } catch (Exception ignored) {}
            }
        }

        return "redirect:" + target;
    }
}
