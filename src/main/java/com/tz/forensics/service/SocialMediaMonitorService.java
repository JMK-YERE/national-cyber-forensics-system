package com.tz.forensics.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Service
public class SocialMediaMonitorService {

    public Map<String, Object> checkFacebook(String username) {
        Map<String, Object> result = new HashMap<>();
        result.put("platform", "Facebook");
        result.put("icon", "👥");
        result.put("username", username);
        result.put("profileLink", "https://www.facebook.com/" + username);
        result.put("securityLink", "https://www.facebook.com/settings?tab=security");
        result.put("recommendations", new String[]{
            "Weka 2FA (Two-Factor Authentication) kwenye Facebook",
            "Weka 'Login Alerts' — utapata taarifa kila mtu anapoingia",
            "Angalia 'Where You're Logged In' — ondoa devices usizozijua",
            "Ondoa apps zisizotumika kwenye 'Apps and Websites'",
            "Weka 'Trusted Contacts' kwa account recovery",
            "Usibonyeze links kutoka kwa DM za watu usiowajua"
        });
        result.put("exists", checkUrl("https://www.facebook.com/" + username));
        return result;
    }

    public Map<String, Object> checkInstagram(String username) {
        Map<String, Object> result = new HashMap<>();
        result.put("platform", "Instagram");
        result.put("icon", "📷");
        result.put("username", username);
        result.put("profileLink", "https://www.instagram.com/" + username + "/");
        result.put("securityLink", "https://www.instagram.com/accounts/two_factor_authentication/");
        result.put("recommendations", new String[]{
            "Weka 2FA kwenye Instagram (Settings → Security → Two-Factor Authentication)",
            "Weka 'Login Activity' alerts",
            "Angalia 'Apps and Websites' — ondoa zisizotumika",
            "Badilisha password mara kwa mara",
            "Weka email na phone ya recovery",
            "Fanya account yako private kama unataka"
        });
        result.put("exists", checkUrl("https://www.instagram.com/" + username + "/"));
        return result;
    }

    public Map<String, Object> checkTikTok(String username) {
        Map<String, Object> result = new HashMap<>();
        result.put("platform", "TikTok");
        result.put("icon", "🎵");
        result.put("username", username);
        result.put("profileLink", "https://www.tiktok.com/@" + username);
        result.put("securityLink", "https://www.tiktok.com/setting/security");
        result.put("recommendations", new String[]{
            "Weka 2FA kwenye TikTok",
            "Weka 'Login Alerts'",
            "Angalia 'Manage Devices' — ondoa devices usizozijua",
            "Usibonyeze links kutoka kwa DM za kutiliwa shaka",
            "Rekebisha privacy settings",
            "Kataa downloads za video zako kama hutaki"
        });
        result.put("exists", checkUrl("https://www.tiktok.com/@" + username));
        return result;
    }

    public Map<String, Object> checkWhatsApp(String phone) {
        Map<String, Object> result = new HashMap<>();
        result.put("platform", "WhatsApp");
        result.put("icon", "💬");
        result.put("username", phone);
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        result.put("profileLink", "https://wa.me/" + cleanPhone);
        result.put("securityLink", "https://faq.whatsapp.com/489853655551903");
        result.put("recommendations", new String[]{
            "Weka 'Two-Step Verification' kwenye WhatsApp (Settings → Account)",
            "Weka 'Fingerprint Lock' kwa app",
            "Angalia 'Linked Devices' — ondoa zisizotumika",
            "Usifungue links kutoka kwa watu usiowajua",
            "Weka 'Privacy' → 'Last Seen' kuwa 'My Contacts'",
            "Kataa 'Unknown Callers' (kama inapatikana)"
        });
        result.put("exists", true);
        return result;
    }

    public Map<String, Object> checkX(String username) {
        Map<String, Object> result = new HashMap<>();
        result.put("platform", "X (Twitter)");
        result.put("icon", "🐦");
        result.put("username", username);
        result.put("profileLink", "https://x.com/" + username);
        result.put("securityLink", "https://twitter.com/settings/security");
        result.put("recommendations", new String[]{
            "Weka 2FA kwenye X (Security → Two-Factor Authentication)",
            "Tumia 'App-based' 2FA (Google Authenticator)",
            "Angalia 'Connected Apps' — ondoa zisizotumika",
            "Weka 'Password Reset Protection'",
            "Angalia 'Login History'",
            "Chagua 'Who can see your email' → 'Only you'"
        });
        result.put("exists", checkUrl("https://x.com/" + username));
        return result;
    }

    public Map<String, Object> checkGmail(String email) {
        Map<String, Object> result = new HashMap<>();
        result.put("platform", "Gmail");
        result.put("icon", "📧");
        result.put("username", email);
        result.put("profileLink", "https://mail.google.com");
        result.put("securityLink", "https://myaccount.google.com/security");
        result.put("recommendations", new String[]{
            "Weka 2FA kwenye Google Account",
            "Angalia 'Recent Security Activity'",
            "Weka 'App Passwords' kwa apps za tatu",
            "Angalia 'Third-party apps' — ondoa zisizotumika",
            "Weka 'Recovery Email' na 'Recovery Phone'",
            "Fanya 'Security Checkup' kila miezi 3",
            "Check email yako kwenye HaveIBeenPwned"
        });
        result.put("exists", true);
        return result;
    }

    private boolean checkUrl(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
            int code = conn.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Exception e) {
            // Kama imefeli (network blocks HEAD), assume exists
            return true;
        }
    }
}
