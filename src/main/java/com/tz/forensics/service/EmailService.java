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

    @Value("${app.name:National Cyber Forensics System}")
    private String appName;

    @Value("${app.url:https://national-cyber-forensics-system-1.onrender.com}")
    private String appUrl;

    private boolean isConfigured() {
        return mailSender != null && fromEmail != null && !fromEmail.isEmpty();
    }

    public void sendEmail(String to, String subject, String body) {
        if (!isConfigured()) {
            System.out.println("⚠️ Email not configured. To: " + to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(appName + " <" + fromEmail + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println("✅ Email sent to " + to);
        } catch (Exception e) {
            System.err.println("❌ Email failed: " + e.getMessage());
        }
    }

    public void sendWelcomeEmail(String to, String username) {
        String subject = "🇹🇿 Karibu " + appName + ", " + username + "!";
        String body = "Habari " + username + ",\n\n"
                + "Karibu kwenye " + appName + "!\n\n"
                + "Akaunti yako imefunguliwa kwa mafanikio.\n\n"
                + "Unaweza:\n"
                + "✅ Kuripoti matukio ya usalama wa mtandao\n"
                + "✅ Kupakia ushahidi wa kidijitali (AES-256)\n"
                + "✅ Kufuatilia chain of custody\n"
                + "✅ Kupata notifications\n\n"
                + "Ingia mfumo:\n"
                + appUrl + "/login\n\n"
                + "Username: " + username + "\n\n"
                + "Asante kwa kujiunga!\n\n"
                + appName + " - Tanzania 🇹🇿";
        sendEmail(to, subject, body);
    }

    public void sendAdminNewUserAlert(String username, String userEmail) {
        if (adminEmail == null || adminEmail.isEmpty()) return;
        String subject = "👤 Mtumiaji Mpya: " + username;
        String body = "Mtumiaji mpya amejiunga:\n\n"
                + "Username: " + username + "\n"
                + "Email: " + userEmail + "\n"
                + "Muda: " + java.time.LocalDateTime.now();
        sendEmail(adminEmail, subject, body);
    }

    public void sendIncidentAlert(String incidentId, String title, String severity) {
        if (adminEmail == null || adminEmail.isEmpty()) return;
        String subject = "🚨 Tukio Jipya: " + incidentId;
        String body = "Tukio jipya:\n\n"
                + "ID: " + incidentId + "\n"
                + "Title: " + title + "\n"
                + "Severity: " + severity + "\n"
                + "Muda: " + java.time.LocalDateTime.now() + "\n\n"
                + appUrl + "/incidents";
        sendEmail(adminEmail, subject, body);
    }

    public void sendIncidentConfirmation(String to, String username, String incidentId, String title) {
        String subject = "✅ Tukio Lako Limepokelewa: " + incidentId;
        String body = "Habari " + username + ",\n\n"
                + "Tukio lako limepokelewa:\n\n"
                + "ID: " + incidentId + "\n"
                + "Title: " + title + "\n\n"
                + "Timu yetu itafuatilia.\n\n"
                + appUrl + "/incidents\n\n"
                + appName + " - Tanzania 🇹🇿";
        sendEmail(to, subject, body);
    }
}
