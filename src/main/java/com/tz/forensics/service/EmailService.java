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

    @Value("${app.name:Cyber Forensics TZ}")
    private String appName;

    @Value("${app.url:https://cyber-forensics-tz.onrender.com}")
    private String appUrl;

    private boolean isConfigured() {
        return mailSender != null && fromEmail != null && !fromEmail.isEmpty();
    }

    public void sendEmail(String to, String subject, String body) {
        if (!isConfigured()) {
            System.out.println("⚠️ Email not configured. Would send to: " + to);
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
            System.err.println("❌ Email failed to " + to + ": " + e.getMessage());
        }
    }

    public void sendPasswordSetupEmail(String to, String username, String token) {
        String subject = "🔐 Tengeneza password yako - " + appName;
        String body = "Habari " + username + ",\n\n"
                + "Akaunti yako ya " + appName + " imepokelewa.\n\n"
                + "Bonyeza link hii kutengeneza password yako:\n"
                + appUrl + "/set-password?token=" + token + "\n\n"
                + "Link hii ita-expire ndani ya saa 24.\n\n"
                + "Kama hukuomba akaunti hii, puuza ujumbe huu.\n\n"
                + appName + " - Tanzania 🇹🇿";
        sendEmail(to, subject, body);
    }

    public void sendVerificationEmail(String to, String username, String token) {
        String subject = "🔐 Thibitisha email yako - " + appName;
        String body = "Habari " + username + ",\n\n"
                + "Thibitisha email yako ili kuamilisha akaunti yako.\n\n"
                + appUrl + "/verify-email?token=" + token + "\n\n"
                + "Link hii ita-expire ndani ya saa 24.\n\n"
                + "Kama hukuomba akaunti hii, puuza ujumbe huu.\n\n"
                + appName + " - Tanzania 🇹🇿";
        sendEmail(to, subject, body);
    }

    public void sendPasswordResetEmail(String to, String username, String token) {
        String subject = "🔑 Reset password - " + appName;
        String body = "Habari " + username + ",\n\n"
                + "Umeomba kutengeneza password mpya.\n\n"
                + appUrl + "/reset-password?token=" + token + "\n\n"
                + "Link hii ita-expire ndani ya saa 1.\n"
                + "Kama hukuomba reset, puuza ujumbe huu.\n\n"
                + appName + " - Tanzania 🇹🇿";
        sendEmail(to, subject, body);
    }

    public void sendOAuthLoginOtp(String to, String username, String otp) {
        String subject = "🔐 Msimbo wa usalama wa kuingia - " + appName;
        String body = "Habari " + username + ",\n\n"
                + "Tumepokea ombi la kuingia kwenye " + appName + " kupitia Google.\n\n"
                + "Msimbo wako wa uthibitisho ni:\n\n"
                + "        " + otp + "\n\n"
                + "Msimbo huu una muda wa sekunde 60 tu na unaweza kutumika mara moja.\n"
                + "Usimpe mtu mwingine msimbo huu. Kama hukuomba kuingia, puuza email hii na badilisha usalama wa akaunti yako.\n\n"
                + appName + " - Tanzania 🇹🇿";
        sendEmail(to, subject, body);
    }

    // ===== KWA MTU MWENYEWE (Welcome) =====
    public void sendWelcomeEmail(String to, String username) {
        String subject = "🇹🇿 Karibu " + appName + ", " + username + "!";
        String body = "Habari " + username + ",\n\n"
                + "Karibu kwenye " + appName + "!\n\n"
                + "Akaunti yako imefunguliwa kwa mafanikio.\n\n"
                + "Unaweza:\n"
                + "✅ Kuripoti matukio ya usalama\n"
                + "✅ Kupakia ushahidi (AES-256)\n"
                + "✅ Kufuatilia chain of custody\n\n"
                + "Ingia mfumo:\n"
                + appUrl + "/login\n\n"
                + "Username: " + username + "\n\n"
                + "Asante,\n"
                + appName + " - Tanzania 🇹🇿";
        sendEmail(to, subject, body);
    }

    // ===== KWA MTU (Incident Confirmation) =====
    public void sendIncidentConfirmation(String to, String username, String incidentId, String title) {
        String subject = "✅ Tukio Lako Limepokelewa: " + incidentId;
        String body = "Habari " + username + ",\n\n"
                + "Tukio lako limepokelewa kwa mafanikio:\n\n"
                + "🆔 Incident ID: " + incidentId + "\n"
                + "📝 Title: " + title + "\n\n"
                + "Timu yetu itafuatilia tukio lako.\n"
                + "Utapata notifications kuhusu maendeleo.\n\n"
                + "Angalia tukio lako:\n"
                + appUrl + "/incidents\n\n"
                + "Asante,\n"
                + appName + " - Tanzania 🇹🇿";
        sendEmail(to, subject, body);
    }

    // ===== KWA ADMIN (Mtumiaji Mpya) =====
    public void sendAdminNewUserAlert(String username, String userEmail) {
        if (adminEmail == null || adminEmail.isEmpty()) return;
        String subject = "👤 Mtumiaji Mpya: " + username;
        String body = "Mtumiaji mpya amejiunga:\n\n"
                + "Username: " + username + "\n"
                + "Email: " + userEmail + "\n"
                + "Muda: " + java.time.LocalDateTime.now();
        sendEmail(adminEmail, subject, body);
    }

    // ===== KWA ADMIN (Incident Mpya) =====
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
}
