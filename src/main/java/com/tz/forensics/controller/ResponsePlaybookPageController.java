package com.tz.forensics.controller;

import com.tz.forensics.service.ResponsePlaybookService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Staff workspace page for browsing approved response guidance. */
@Controller
public class ResponsePlaybookPageController {
    private final ResponsePlaybookService playbooks;

    public ResponsePlaybookPageController(ResponsePlaybookService playbooks) {
        this.playbooks = playbooks;
    }

    @GetMapping("/response-playbooks")
    public String page(Model model, Authentication authentication) {
        model.addAttribute("playbooks", playbooks.list());
        model.addAttribute("currentUsername", authentication == null ? "" : authentication.getName());
        return "response-playbooks";
    }
}
