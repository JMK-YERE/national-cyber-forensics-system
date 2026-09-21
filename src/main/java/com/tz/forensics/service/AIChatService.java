package com.tz.forensics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIChatService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public String chat(String userMessage, String context) {
        if (!isConfigured()) {
            return fallbackResponse(userMessage);
        }

        try {
            // System prompt — AI inaweza kujibu KILA KITU
            String systemPrompt = "Wewe ni AI Assistant wa National Cyber Forensics System - Tanzania. "
                + "Unaweza kusaidia kwa KILA KITU: cybersecurity, digital forensics, mfumo huu, "
                + "maswali ya kawaida, ushauri wa kazi, maisha, elimu, tech, biashara, na zaidi. "
                + "Jibu kwa Kiswahili (au Kiingereza kama mtumiaji anatumia Kiingereza). "
                + "Kuwa rafiki, msaidizi, na wa kitaalamu. "
                + "Kama ni swali la kiufundi, toa maelezo ya hatua kwa hatua. "
                + "Kama ni swali la kawaida, jibu kwa ufupi (sentensi 2-5). "
                + "Kama ni hadithi au stori, jibu kwa ubunifu. "
                + "Context ya mtumiaji: " + (context != null ? context : "Hakuna");

            String jsonBody = buildJsonRequest(systemPrompt + "\n\nMtumiaji: " + userMessage);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            String url = BASE_URL + model + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractText(response.body());
            } else {
                System.err.println("Gemini API error: " + response.statusCode() + " - " + response.body());
                return fallbackResponse(userMessage);
            }
        } catch (Exception e) {
            System.err.println("AI Chat failed: " + e.getMessage());
            return fallbackResponse(userMessage);
        }
    }

    private String buildJsonRequest(String prompt) {
        String escaped = prompt.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
        return "{\"contents\":[{\"parts\":[{\"text\":\"" + escaped + "\"}]}]}";
    }

    private String extractText(String jsonResponse) {
        try {
            Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            Matcher matcher = pattern.matcher(jsonResponse);
            if (matcher.find()) {
                String text = matcher.group(1);
                return text.replace("\\n", "\n")
                           .replace("\\\"", "\"")
                           .replace("\\\\", "\\")
                           .replace("\\r", "");
            }
        } catch (Exception e) {
            System.err.println("Parse failed: " + e.getMessage());
        }
        return "Samahani, sikupata jibu sahihi. Jaribu tena.";
    }

    // ===== FALLBACK (bila API key) =====
    private String fallbackResponse(String message) {
        String lower = message.toLowerCase();

        // Security questions
        if (lower.contains("password")) {
            return "Kwa password nzuri:\n✅ Herufi 12+\n✅ Kubwa na ndogo (A-Z, a-z)\n✅ Namba (0-9)\n✅ Alama (!@#$%)\n❌ Usitumie 'password123' au jina lako\n\nMfano: 'Tz#Cyber2024!'";
        }
        if (lower.contains("phishing")) {
            return "Phishing ni email/SMS za kudanganya:\n🎣 Zinaomba password/OTP\n🎣 Zina links za kutiliwa shaka\n🎣 Zinatisha (account itafungwa)\n\n✅ Angalia URL vizuri (https://)\n✅ Usibonyeze links\n✅ Ripoti kwa platform";
        }
        if (lower.contains("2fa") || lower.contains("two factor")) {
            return "2FA (Two-Factor Authentication):\n\n📱 Kwenye Facebook/IG/Gmail:\n1. Settings → Security\n2. 'Two-Factor Authentication'\n3. Chagua 'Authentication App'\n4. Download Google Authenticator\n5. Scan QR code\n\n✅ Hii inazuia 99% ya hacking!";
        }
        if (lower.contains("ransomware")) {
            return "Ransomware inafunga files zako:\n\n🛡️ Kinga:\n✅ Backup data kila wiki\n✅ Sasisha OS na apps\n✅ Usifungue attachments za kutiliwa shaka\n✅ Weka antivirus\n\n🚨 Kama umeshambuliwa:\n❌ USILIpe fidia\n✅ Ripoti Polisi (112)\n✅ Wasiliana na IT team";
        }
        if (lower.contains("whatsapp")) {
            return "WhatsApp Security:\n\n📱 Weka Two-Step Verification:\n1. Settings → Account\n2. 'Two-step verification' → Enable\n3. Weka PIN ya tarakimu 6\n\n✅ Angalia 'Linked Devices'\n✅ Weka Fingerprint Lock\n✅ Usifungue links za kutiliwa shaka";
        }
        if (lower.contains("facebook") || lower.contains("instagram")) {
            return "Meta Platforms Security:\n\n📱 Settings → Security:\n✅ Weka 2FA\n✅ Weka Login Alerts\n✅ Angalia 'Where You're Logged In'\n✅ Ondoa apps zisizotumika\n\n🚨 Kama account imehack:\n✅ Tumia 'Forgot Password'\n✅ Wasiliana Support mara moja";
        }
        if (lower.contains("halo") || lower.contains("habari") || lower.contains("hello") || lower.contains("hi")) {
            return "Habari! 👋\n\nMimi ni AI Assistant wa Cyber Forensics TZ. Naweza kukusaidia kwa:\n\n🔐 Security — password, 2FA, phishing\n💼 Kazi — ushauri wa kazi\n📚 Elimu — kujifunza\n💡 Maisha — maswali ya kawaida\n🤖 Tech — maelezo ya technolojia\n\nUliza swali lolote!";
        }
        if (lower.contains("asante") || lower.contains("thanks")) {
            return "Karibu sana! 😊\n\nKama una swali lingine — kuhusu security, kazi, maisha, au kitu chochote — nipo hapa kukusaidia.\n\n🛡️ Karibu tena!";
        }
        if (lower.contains("kazi") || lower.contains("job") || lower.contains("cv") || lower.contains("resume")) {
            return "Ushauri wa Kazi:\n\n📝 CV nzuri:\n✅ Ukurasa 1-2\n✅ Achievements, sio responsibilities\n✅ Skills zinazohitajika\n✅ Contact info sahihi\n\n💼 Interviews:\n✅ Jifunze kuhusu kampuni\n✅ Andaa maswali\n✅ Vaavyo vizuri\n✅ Fika mapema\n\nUliza swali mahususi kwa msaada zaidi!";
        }
        if (lower.contains("biashara") || lower.contains("business")) {
            return "Ushauri wa Biashara:\n\n💡 Anza kidogo:\n✅ Capital ndogo\n✅ Test market kwanza\n✅ Jifunze kutoka wateja\n\n📈 Kukua:\n✅ Fanya marketing\n✅ Weka bei sahihi\n✅ Toa service nzuri\n\nUliza swali mahususi!";
        }
        if (lower.contains("kusoma") || lower.contains("study") || lower.contains("shule")) {
            return "Ushauri wa Kusoma:\n\n📚 Njia nzuri:\n✅ Study kwa mfumo (Pomodoro 25 min)\n✅ Andika notes\n✅ Fanya mazoezi\n✅ Pumzika vizuri\n✅ Jifunze kwa marafiki\n\nUliza kuhusu subject mahususi!";
        }
        if (lower.contains("afya") || lower.contains("health")) {
            return "Ushauri wa Afya:\n\n💪 Kwa maisha mazuri:\n✅ Kula vyakula bora\n✅ Mazoezi kila siku (dakika 30)\n✅ Kunywa maji mengi\n✅ Lala masaa 7-8\n✅ Punguza stress\n\n⚠️ Kwa matatizo ya afya — muone daktari!";
        }

        // Default
        return "Samahani, kwa sasa AI API haijawekwa. Lakini naweza kujibu maswali ya kawaida:\n\n"
            + "🔐 Security — password, 2FA, phishing, ransomware\n"
            + "💼 Kazi — CV, interview, kazi\n"
            + "📚 Elimu — kusoma, masomo\n"
            + "💡 Maisha — ushauri wa kawaida\n"
            + "🤖 Tech — technolojia\n\n"
            + "Uliza swali lolote — nitajaribu kukusaidia!";
    }
}
