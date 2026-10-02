package com.tz.forensics.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DownloadsController {

    @GetMapping("/downloads")
    public String downloads() {
        return "downloads";
    }
}
