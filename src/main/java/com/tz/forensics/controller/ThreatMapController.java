package com.tz.forensics.controller;

import com.tz.forensics.repository.ReportAttackRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.*;

@Controller
public class ThreatMapController {
    private final ReportAttackRepository reportRepository;
    private final UserRepository userRepository;
    public ThreatMapController(ReportAttackRepository reportRepository, UserRepository userRepository){
        this.reportRepository=reportRepository;
        this.userRepository=userRepository;
    }

    @GetMapping("/threat-map")
    public String threatMap(Authentication auth, Model model) {
        boolean allowed = auth != null && auth.getAuthorities().stream().anyMatch(a -> java.util.Set.of("ROLE_ADMIN","ROLE_CYBER_PRO","ROLE_FORENSICS","ROLE_ANALYST").contains(a.getAuthority()));
        if (!allowed) return "redirect:/access-denied";
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/access-denied";
        var reports = (user.isAdmin() || user.isProfessional() || user.isForensics())
                ? reportRepository.findAll()
                : reportRepository.findByAssignedToOrderByCreatedAtDesc(user.getId());

        List<Map<String,Object>> points = new ArrayList<>();
        reports.forEach(r -> {
            if (r.getRegion() == null || r.getRegion().isBlank()) return;
            Map<String,Object> p = new LinkedHashMap<>();
            p.put("region", r.getRegion());
            p.put("priority", r.getPriority() == null ? "MEDIUM" : r.getPriority());
            p.put("type", r.getAttackTypeLabel());
            points.add(p);
        });
        model.addAttribute("mapPoints", points);
        return "threat-map";
    }
}
