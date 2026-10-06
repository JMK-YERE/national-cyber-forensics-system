package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AIChatService;
import com.tz.forensics.service.AuditService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/ai")
public class AIController {

    private final AIChatService aiChatService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    private static final String SESSION_HISTORY = "CFTZ_AI_HISTORY";
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final int MAX_HISTORY_ENTRIES = 40;

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
        model.addAttribute("history", getHistory(model, auth));
        model.addAttribute("isConfigured", aiChatService.isConfigured());
        model.addAttribute("currentLang", LocaleContextHolder.getLocale().getLanguage());
        return "ai-assistant";
    }

    @PostMapping("/assistant")
    public String askAI(@RequestParam String message, Authentication auth, HttpSession session) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        String username = auth.getName();
        String normalizedMessage = message == null ? "" : message.trim();
        if (normalizedMessage.isBlank()) return "redirect:/ai/assistant";
        if (normalizedMessage.length() > MAX_MESSAGE_LENGTH) {
            normalizedMessage = normalizedMessage.substring(0, MAX_MESSAGE_LENGTH);
        }
        String currentLang = LocaleContextHolder.getLocale().getLanguage();
        String context = buildContext(user);

        // Pass language kwenye AI
        String response = aiChatService.chat(normalizedMessage, context, currentLang);

        List<Map<String, String>> history = getSessionHistory(session);
        history.add(createChatEntry("user", normalizedMessage));
        history.add(createChatEntry("ai", response == null ? "" : response));
        if (history.size() > MAX_HISTORY_ENTRIES) {
            history.subList(0, history.size() - MAX_HISTORY_ENTRIES).clear();
        }
        session.setAttribute(SESSION_HISTORY, history);

        auditService.log("AI_QUERY", "AIChat", username,
                "Lang: " + currentLang + " | QueryLength: " + normalizedMessage.length());

        return "redirect:/ai/assistant";
    }

    @PostMapping("/clear")
    public String clearHistory(Authentication auth) {
        chatHistory.remove(auth.getName());
        return "redirect:/ai/assistant";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getSessionHistory(HttpSession session) {
        Object value = session.getAttribute(SESSION_HISTORY);
        if (value instanceof List<?>) {
            return (List<Map<String, String>>) value;
        }
        return new ArrayList<>();
    }

    private List<Map<String, String>> getHistory(Model model, Authentication auth) {
        return new ArrayList<>();
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
