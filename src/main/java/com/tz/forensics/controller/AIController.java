package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AIChatService;
import com.tz.forensics.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/ai")
public class AIController {

    private final AIChatService aiChatService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // Simple in-memory chat history per user (kwa demo)
    private static final Map<String, List<Map<String, String>>> chatHistory = new HashMap<>();

    public AIController(AIChatService aiChatService, UserRepository userRepository, AuditService auditService) {
        this.aiChatService = aiChatService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping("/assistant")
    public String assistant(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("history", chatHistory.getOrDefault(auth.getName(), new ArrayList<>()));
        model.addAttribute("isConfigured", aiChatService.isConfigured());
        return "ai-assistant";
    }

    @PostMapping("/assistant")
    public String askAI(@RequestParam String message, Authentication auth, RedirectAttributes ra) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        // Get context kuhusu user
        String context = buildContext(user);

        // Get AI response
        String response = aiChatService.chat(message, context);

        // Save kwenye history
        List<Map<String, String>> history = chatHistory.computeIfAbsent(username, k -> new ArrayList<>());
        history.add(createChatEntry("user", message));
        history.add(createChatEntry("ai", response));

        // Keep last 20 messages
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }

        auditService.log("AI_QUERY", "AIChat", username, "Query: " + message.substring(0, Math.min(50, message.length())));

        return "redirect:/ai/assistant";
    }

    @PostMapping("/clear")
    public String clearHistory(Authentication auth) {
        chatHistory.remove(auth.getName());
        return "redirect:/ai/assistant";
    }

    private Map<String, String> createChatEntry(String type, String text) {
        Map<String, String> entry = new HashMap<>();
        entry.put("type", type);
        entry.put("text", text);
        entry.put("time", java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        return entry;
    }

    private String buildContext(User user) {
        if (user == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("User: ").append(user.getUsername()).append(". ");
        sb.append("Role: ").append(user.getRole()).append(". ");
        if (user.getOrganization() != null) sb.append("Organization: ").append(user.getOrganization()).append(". ");
        return sb.toString();
    }
}
