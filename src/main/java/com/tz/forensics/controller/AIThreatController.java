package com.tz.forensics.controller;

import com.tz.forensics.service.AIThreatDetectionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class AIThreatController {

    private final AIThreatDetectionService threatService;

    public AIThreatController(AIThreatDetectionService threatService) {
        this.threatService = threatService;
    }

    @GetMapping("/ai/threat-detection")
    public String threatDetection(Model model) {
        Map<String, Object> analysis = threatService.analyzeThreats();
        Map<String, Object> prediction = threatService.predictThreats();

        model.addAttribute("analysis", analysis);
        model.addAttribute("prediction", prediction);

        return "ai-threat-detection";
    }

    @PostMapping("/ai/analyze-incident")
    @ResponseBody
    public Map<String, Object> analyzeIncident(@RequestParam String title,
                                                 @RequestParam String description) {
        return threatService.analyzeIncident(title, description);
    }

    @GetMapping("/ai/analysis-api")
    @ResponseBody
    public Map<String, Object> analysisApi() {
        return threatService.analyzeThreats();
    }

    @GetMapping("/ai/prediction-api")
    @ResponseBody
    public Map<String, Object> predictionApi() {
        return threatService.predictThreats();
    }
}
