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
                                  @RequestParam(required = false, defaultValue = "/dashboard") String redirect,
                                  HttpServletResponse response) {
        Cookie cookie = new Cookie("lang", lang);
        cookie.setMaxAge(365 * 24 * 60 * 60); // 1 year
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:" + redirect;
    }
}
