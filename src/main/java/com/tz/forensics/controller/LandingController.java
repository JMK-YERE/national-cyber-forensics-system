package com.tz.forensics.controller;

import com.tz.forensics.service.LandingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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

    @GetMapping(value = "/favicon.ico", produces = "image/svg+xml")
    public ResponseEntity<String> favicon() {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\"><rect width=\"64\" height=\"64\" rx=\"14\" fill=\"#063b2c\"/><path d=\"M32 8l20 8v15c0 13-8 22-20 25C20 53 12 44 12 31V16l20-8z\" fill=\"#087d68\"/><path d=\"M32 18l10 4v9c0 7-4 12-10 15-6-3-10-8-10-15v-9l10-4z\" fill=\"white\"/></svg>";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("image/svg+xml")).body(svg);
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
