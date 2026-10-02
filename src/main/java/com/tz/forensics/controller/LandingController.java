package com.tz.forensics.controller;

import com.tz.forensics.service.LandingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    private static final Logger log = LoggerFactory.getLogger(LandingController.class);

    private final LandingService landingService;

    public LandingController(LandingService landingService) {
        this.landingService = landingService;
    }

    @GetMapping("/")
    public String landing(Model model) {
        try {
            var announcements = landingService.getActiveAnnouncements();
            var slides = landingService.getActiveSlides();
            log.info("Landing page: {} announcements, {} slides", announcements.size(), slides.size());
            model.addAttribute("announcements", announcements);
            model.addAttribute("slides", slides);
        } catch (Exception e) {
            log.error("Landing page error: {}", e.getMessage(), e);
            model.addAttribute("announcements", java.util.List.of());
            model.addAttribute("slides", java.util.List.of());
        }
        return "landing";
    }

    @GetMapping("/features")
    public String features() {
        return "landing";
    }

    @GetMapping("/about")
    public String about() {
        return "landing";
    }

    @GetMapping("/contact")
    public String contact() {
        return "landing";
    }
}
