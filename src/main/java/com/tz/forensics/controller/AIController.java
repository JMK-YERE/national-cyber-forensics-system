package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AIChatService;
import com.tz.forensics.service.AuditService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/ai")
public class AIController {

    private final AIChatService aiChatService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    private static final Map<String, List<Map<String, String>>> chatHistory = new HashMap<>();

    public AIController(AIChatService aiChatService, UserRepository userRepository, AuditService auditService) {
        this.aiChatService = aiChatService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping("/assistant")
    public String assistant(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("history", chatHistory.getOrDefault(auth.getName(), new ArrayList<>()));
        model.addAttribute("isConfigured", aiChatService.isConfigured());
        model.addAttribute("currentLang", LocaleContextHolder.getLocale().getLanguage());
        return "ai-assistant";
    }

    @PostMapping("/assistant")
    public String askAI(@RequestParam String message, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        String username = auth.getName();
        String currentLang = LocaleContextHolder.getLocale().getLanguage();
        String context = buildContext(user);

        // Pass language kwenye AI
        String response = aiChatService.chat(message, context, currentLang);

        List<Map<String, String>> history = chatHistory.computeIfAbsent(username, k -> new ArrayList<>());
        history.add(createChatEntry("user", message));
        history.add(createChatEntry("ai", response));

        if (history.size() > 40) {
            history.subList(0, history.size() - 40).clear();
        }

        auditService.log("AI_QUERY", "AIChat", username,
                "Lang: " + currentLang + " | Query: " + message.substring(0, Math.min(50, message.length())));

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
        if (user.getOrganization() != null) sb.append("Org: ").append(user.getOrganization()).append(". ");
        return sb.toString();
    }
}
