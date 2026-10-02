package com.tz.forensics.controller;

import com.tz.forensics.service.LandingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    private final LandingService landingService;

    public LandingController(LandingService landingService) {
        this.landingService = landingService;
    }

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("announcements", landingService.getActiveAnnouncements());
        model.addAttribute("slides", landingService.getActiveSlides());
        return "landing";
    }

    @GetMapping("/features")
    public String features() {
        return "landing-features";
    }
}
