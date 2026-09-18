package com.tz.forensics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.admin.email:}")
    private String adminEmail;

    private boolean isConfigured() {
        return mailSender != null && fromEmail != null && !fromEmail.isEmpty();
    }

    public void sendEmail(String to, String subject, String body) {
        if (!isConfigured()) {
            System.out.println("⚠️ Email not configured.");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println("✅ Email sent to " + to);
        } catch (Exception e) {
            System.err.println("❌ Email failed: " + e.getMessage());
        }
    }

    public void sendIncidentAlert(String incidentId, String title, String severity) {
        if (adminEmail == null || adminEmail.isEmpty()) return;
        String subject = "🚨 Tukio Jipya: " + incidentId;
        String body = "Tukio jipya limepokelewa:\n\n"
                + "Incident ID: " + incidentId + "\n"
                + "Title: " + title + "\n"
                + "Severity: " + severity + "\n\n"
                + "Ingia mfumo kwa maelezo zaidi.";
        sendEmail(adminEmail, subject, body);
    }

    public void sendWelcomeEmail(String to, String username) {
        String subject = "🇹🇿 Karibu Cyber Forensics TZ";
        String body = "Habari " + username + ",\n\nAkaunti yako imefunguliwa.\nAsante.";
        sendEmail(to, subject, body);
    }
}
