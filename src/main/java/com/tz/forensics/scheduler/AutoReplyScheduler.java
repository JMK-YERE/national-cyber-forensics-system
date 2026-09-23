package com.tz.forensics.scheduler;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.ReportMessage;
import com.tz.forensics.repository.ReportAttackRepository;
import com.tz.forensics.repository.ReportMessageRepository;
import com.tz.forensics.service.AIChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class AutoReplyScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoReplyScheduler.class);

    private final ReportAttackRepository reportRepo;
    private final ReportMessageRepository messageRepo;
    private final AIChatService aiChatService;

    // ===== DAKIKA 5 — Admin hajajibu, AI inajibu =====
    private static final int MINUTES_TO_WAIT = 5;

    public AutoReplyScheduler(ReportAttackRepository reportRepo,
                                ReportMessageRepository messageRepo,
                                AIChatService aiChatService) {
        this.reportRepo = reportRepo;
        this.messageRepo = messageRepo;
        this.aiChatService = aiChatService;
    }

    @Scheduled(fixedRate = 2 * 60 * 1000)
    public void checkPendingReports() {
        log.info("=== AI Auto-Reply Check ===");

        LocalDateTime cutoff = LocalDateTime.now().minus(MINUTES_TO_WAIT, ChronoUnit.MINUTES);
        List<ReportAttack> allReports = reportRepo.findAll();

        for (ReportAttack report : allReports) {
            try {
                if ("CLOSED".equals(report.getStatus()) || "RESOLVED".equals(report.getStatus())) continue;
                if (report.getCreatedAt() == null || report.getCreatedAt().isAfter(cutoff)) continue;

                List<ReportMessage> messages = messageRepo.findByReportIdOrderByCreatedAtAsc(report.getId());

                boolean hasAdminReply = messages.stream().anyMatch(m -> "ADMIN".equals(m.getSenderType()));
                boolean hasAiReply = messages.stream().anyMatch(m -> "AI".equals(m.getSenderType()));

                // Admin hajajibu na AI hajajibu — AI inajibu
                if (!hasAdminReply && !hasAiReply) {
                    sendAiAutoReply(report);
                }
                // User ameuliza jipya baada ya AI reply
                else if (!hasAdminReply && hasAiReply) {
                    LocalDateTime lastAiTime = messages.stream()
                            .filter(m -> "AI".equals(m.getSenderType()))
                            .map(ReportMessage::getCreatedAt)
                            .max(LocalDateTime::compareTo).orElse(null);

                    LocalDateTime lastUserTime = messages.stream()
                            .filter(m -> "USER".equals(m.getSenderType()))
                            .map(ReportMessage::getCreatedAt)
                            .max(LocalDateTime::compareTo).orElse(null);

                    if (lastUserTime != null && lastAiTime != null
                            && lastUserTime.isAfter(lastAiTime)
                            && lastUserTime.isBefore(cutoff)) {
                        sendAiAutoReplyToUser(report, messages);
                    }
                }
            } catch (Exception e) {
                log.error("Error report {}: {}", report.getId(), e.getMessage());
            }
        }
    }

    private void sendAiAutoReply(ReportAttack report) {
        try {
            String prompt = "Report Type: " + report.getAttackTypeLabel() + "\n"
                    + "Title: " + report.getTitle() + "\n"
                    + "Description: " + report.getDescription() + "\n"
                    + "Country: " + report.getCountryName() + "\n"
                    + "Region: " + report.getRegion() + "\n\n"
                    + "Jibu kwa Kiswahili kwa mtindo huu:\n"
                    + "1. Asante kwa kuripoti\n"
                    + "2. Case yako inafanyiwa kazi (investigation started)\n"
                    + "3. Hatua 3-4 za haraka zenye namba\n"
                    + "4. Namba za msaada (Polisi 112/999)\n"
                    + "Kuwa mfupi (sentensi 5-7), wa kitaalamu, tumia emoji.";

            String aiResponse = aiChatService.chat(prompt, "AutoReply", "sw");

            messageRepo.save(new ReportMessage(
                report.getId(), null, "AI Assistant", "AI", aiResponse
            ));

            log.info("✅ AI auto-reply sent: {} (5 min elapsed)", report.getReportId());
        } catch (Exception e) {
            log.error("AI auto-reply failed: {}", e.getMessage());
        }
    }

    private void sendAiAutoReplyToUser(ReportAttack report, List<ReportMessage> messages) {
        try {
            String lastUserMessage = messages.stream()
                    .filter(m -> "USER".equals(m.getSenderType()))
                    .reduce((a, b) -> b)
                    .map(ReportMessage::getMessage).orElse("");

            String context = "Report: " + report.getAttackTypeLabel() + " | Status: " + report.getStatus();

            String aiResponse = aiChatService.chat(
                "User ameuliza: " + lastUserMessage
                + "\n\nJibu kwa Kiswahili (sentensi 3-4) kwa kitaalamu. "
                + "Mwambie admin ataendelea kujibu hivi karibuni.", context, "sw"
            );

            messageRepo.save(new ReportMessage(
                report.getId(), null, "AI Assistant", "AI", aiResponse
            ));

            log.info("✅ AI reply to user question: {}", report.getReportId());
        } catch (Exception e) {
            log.error("AI reply to user failed: {}", e.getMessage());
        }
    }
}
