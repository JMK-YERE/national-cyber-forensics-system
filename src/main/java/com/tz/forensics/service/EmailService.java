package com.tz.forensics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.name:Cyber Forensics TZ}")
    private String appName;

    @Value("${app.url:https://cyber-forensics-tz.onrender.com}")
    private String appUrl;

    @Value("${email.provider:auto}")
    private String emailProvider;

    @Value("${email.resend.api-key:}")
    private String resendApiKey;

    @Value("${email.resend.from:}")
    private String resendFrom;

    public boolean isConfigured() {
        return resendConfigured() || smtpConfigured();
    }

    private boolean smtpConfigured() {
        return mailSender != null
                && fromEmail != null && !fromEmail.isBlank()
                && mailHost != null && !mailHost.isBlank()
                && mailPassword != null && !mailPassword.isBlank();
    }

    private boolean resendConfigured() {
        return resendApiKey != null && !resendApiKey.isBlank()
                && resendFrom != null && !resendFrom.isBlank();
    }

    public boolean sendEmail(String to, String subject, String body) {
        String provider = emailProvider == null ? "auto" : emailProvider.trim().toLowerCase();

        if (("resend".equals(provider) || "auto".equals(provider)) && resendConfigured()) {
            if (sendViaResend(to, subject, body)) return true;
            if ("resend".equals(provider)) return false;
        }

        if (smtpConfigured()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                System.out.println("✅ Email sent via SMTP to " + to);
                return true;
            } catch (Exception e) {
                Throwable root = e;
                while (root.getCause() != null && root.getCause() != root) root = root.getCause();
                System.err.println("❌ SMTP email failed to " + to
                        + " | root=" + root.getClass().getSimpleName()
                        + " | message=" + String.valueOf(root.getMessage()));
            }
        }

        System.err.println("❌ No usable email provider configured for " + to
                + ". Configure RESEND_API_KEY + RESEND_FROM_EMAIL on Render.");
        return false;
    }

    private boolean sendViaResend(String to, String subject, String body) {
        try {
            String json = "{"
                    + "\"from\":\"" + jsonEscape(resendFrom) + "\","
                    + "\"to\":[\"" + jsonEscape(to) + "\"],"
                    + "\"subject\":\"" + jsonEscape(subject) + "\","
                    + "\"text\":\"" + jsonEscape(body) + "\""
                    + "}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("✅ Email sent via Resend to " + to);
                return true;
            }

            System.err.println("❌ Resend rejected email to " + to
                    + " | HTTP " + response.statusCode()
                    + " | response=" + response.body());
            return false;
        } catch (Exception e) {
            System.err.println("❌ Resend request failed"
                    + " | type=" + e.getClass().getSimpleName()
                    + " | message=" + String.valueOf(e.getMessage()));
            return false;
        }
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
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

    public boolean sendPasswordResetOtp(String to, String username, String otp) {
        String subject = "🔑 OTP ya kubadilisha password - " + appName;
        String body = "Habari " + username + ",\n\n"
                + "Tumepokea ombi la kubadilisha password ya akaunti yako ya " + appName + ".\n\n"
                + "OTP yako ya uthibitisho ni:\n\n"
                + "        " + otp + "\n\n"
                + "OTP hii ita-expire ndani ya sekunde 60 na inaweza kutumika mara moja.\n"
                + "Usimpe mtu mwingine OTP hii. Kama hukuomba kubadilisha password, puuza email hii.\n\n"
                + appName + " - Tanzania 🇹🇿";
        return sendEmail(to, subject, body);
    }

    public boolean sendOAuthLoginOtp(String to, String username, String otp) {
        String subject = "🔐 Msimbo wa usalama wa kuingia - " + appName;
        String body = "Habari " + username + ",\n\n"
                + "Tumepokea ombi la kuingia kwenye " + appName + " kupitia Google.\n\n"
                + "Msimbo wako wa uthibitisho ni:\n\n"
                + "        " + otp + "\n\n"
                + "Msimbo huu una muda wa sekunde 60 tu na unaweza kutumika mara moja.\n"
                + "Usimpe mtu mwingine msimbo huu. Kama hukuomba kuingia, puuza email hii na badilisha usalama wa akaunti yako.\n\n"
                + appName + " - Tanzania 🇹🇿";
        return sendEmail(to, subject, body);
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
