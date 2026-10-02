package com.tz.forensics.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ThreatMapController {

    @GetMapping("/threat-map")
    public String threatMap() {
        return "threat-map";
    }
}
