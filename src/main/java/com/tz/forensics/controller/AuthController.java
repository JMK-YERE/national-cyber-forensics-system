package com.tz.forensics.controller;

import com.tz.forensics.dto.UserRegistrationDto;
import com.tz.forensics.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            @RequestParam(required = false) String registered,
                            @RequestParam(required = false) String lang,
                            Model model) {
        if (error != null) model.addAttribute("error", "Username au password si sahihi!");
        if (logout != null) model.addAttribute("message", "Umetoka kwa mafanikio.");
        if (registered != null) model.addAttribute("success",
                "✅ Usajili umefanikiwa! Angalia email yako.");
        if (lang != null) model.addAttribute("selectedLang", lang);
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") UserRegistrationDto dto, Model model) {
        String response = userService.registerUser(dto);
        if ("SUCCESS".equals(response)) {
            return "redirect:/login?registered=true";
        }
        if ("SMS_SENT".equals(response)) {
            return "redirect:/set-password?phone=" + java.net.URLEncoder.encode(dto.getPhone(), java.nio.charset.StandardCharsets.UTF_8);
        }
        if ("SMS_NOT_SENT".equals(response)) {
            model.addAttribute("error", "SMS haijatumwa. Weka namba sahihi au chagua Email.");
            return "register";
        } else {
            model.addAttribute("error", response);
            return "register";
        }
    }
}
