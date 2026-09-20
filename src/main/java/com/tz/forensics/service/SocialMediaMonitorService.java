package com.tz.forensics.service;

import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Service
public class SocialMediaMonitorService {

    // ===== FACEBOOK PROFILE CHECK =====
    public Map<String, Object> checkFacebook(String username) {
        return checkSocialAccount(
            "Facebook",
            "👥",
            "https://www.facebook.com/" + username,
            "https://www.facebook.com/settings?tab=security",
            username,
            new String[]{
                "Weka 2FA kwenye Facebook",
                "Angalia 'Login Alerts' kwenye settings",
                "Ondoa apps zisizotumika kwenye 'Apps and Websites'",
                "Angalia 'Where You're Logged In' na uondoe devices usizozijua",
                "Weka 'Trusted Contacts' kwa account recovery"
            },
            "blue"
        );
    }

    // ===== INSTAGRAM PROFILE CHECK =====
    public Map<String, Object> checkInstagram(String username) {
        return checkSocialAccount(
            "Instagram",
            "📷",
            "https://www.instagram.com/" + username,
            "https://www.instagram.com/accounts/two_factor_authentication/",
            username,
            new String[]{
                "Weka 2FA kwenye Instagram (Settings → Security)",
                "Weka 'Login Activity' alerts",
                "Angalia 'Apps and Websites' na uondoe zisizotumika",
                "Badilisha password mara kwa mara",
                "Weka email na phone ya recovery",
                "Fanya account yako private kama unataka"
            },
            "pink"
        );
    }

    // ===== TIKTOK PROFILE CHECK =====
    public Map<String, Object> checkTikTok(String username) {
        return checkSocialAccount(
            "TikTok",
            "🎵",
            "https://www.tiktok.com/@" + username,
            "https://www.tiktok.com/setting/security",
            username,
            new String[]{
                "Weka 2FA kwenye TikTok",
                "Weka 'Login Alerts'",
                "Angalia 'Manage Devices' na uondoe devices usizozijua",
                "Usibonyeze links kutoka kwa DM za kutiliwa shaka",
                "Rekebisha privacy settings"
            },
            "black"
        );
    }

    // ===== WHATSAPP CHECK =====
    public Map<String, Object> checkWhatsApp(String phone) {
        Map<String, Object> result = new HashMap<>();
        result.put("platform", "WhatsApp");
        result.put("icon", "💬");
        result.put("username", phone);
        result.put("profileLink", "https://wa.me/" + phone.replaceAll("[^0-9]", ""));
        result.put("securityLink", "https://faq.whatsapp.com/489853655551903");
        result.put("found", true);
        result.put("color", "green");
        result.put("recommendations", new String[]{
            "Weka 'Two-Step Verification' kwenye WhatsApp",
            "Weka 'Fingerprint Lock' kwa app",
            "Angalia 'Linked Devices' na uondoe zisizotumika",
            "Usifungue links kutoka kwa watu usiowajua",
            "Weka 'Privacy' → 'Last Seen' kuwa 'My Contacts'",
            "Kataa 'Unknown Callers' (kama inapatikana)"
        });
        return result;
    }

    // ===== X (TWITTER) CHECK =====
    public Map<String, Object> checkX(String username) {
        return checkSocialAccount(
            "X (Twitter)",
            "🐦",
            "https://x.com/" + username,
            "https://twitter.com/settings/security",
            username,
            new String[]{
                "Weka 2FA kwenye X (Security → Two-Factor Authentication)",
                "Tumia 'App-based' 2FA (Google Authenticator)",
                "Angalia 'Connected Apps' na uondoe zisizotumika",
                "Weka 'Password Reset Protection'",
                "Angalia 'Login History'",
                "Chagua 'Who can see your email' → 'Only you'"
            },
            "blue"
        );
    }

    // ===== GMAIL CHECK =====
    public Map<String, Object> checkGmail(String email) {
        Map<String, Object> result = new HashMap<>();
        result.put("platform", "Gmail");
        result.put("icon", "📧");
        result.put("username", email);
        result.put("profileLink", "https://mail.google.com");
        result.put("securityLink", "https://myaccount.google.com/security");
        result.put("found", true);
        result.put("color", "red");
        result.put("recommendations", new String[]{
            "Weka 2FA kwenye Google Account",
            "Angalia 'Recent Security Activity'",
            "Weka 'App Passwords' kwa apps za tatu",
            "Angalia 'Third-party apps' na uondoe zisizotumika",
            "Weka 'Recovery Email' na 'Recovery Phone'",
            "Fanya 'Security Checkup' kila miezi 3",
            "Check email yako kwenye HaveIBeenPwned"
        });
        return result;
    }

    // ===== GENERIC SOCIAL ACCOUNT CHECK =====
    private Map<String, Object> checkSocialAccount(String platform, String icon,
                                                     String profileUrl, String securityUrl,
                                                     String username, String[] recommendations,
                                                     String color) {
        Map<String, Object> result = new HashMap<>();
        result.put("platform", platform);
        result.put("icon", icon);
        result.put("username", username);
        result.put("profileLink", profileUrl);
        result.put("securityLink", securityUrl);
        result.put("color", color);

        // Check kama profile inafunguka
        boolean found = checkUrlExists(profileUrl);
        result.put("found", found);
        result.put("message", found
                ? "✅ Account inaonekana kwenye " + platform
                : "⚠️ Hatukuweza kuthibitisha account — inaweza kuwa private au haipo");

        result.put("recommendations", recommendations);
        return result;
    }

    // ===== URL EXISTS CHECK =====
    private boolean checkUrlExists(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            int code = conn.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Exception e) {
            return false;
        }
    }

    // ===== CALCULATE SECURITY SCORE =====
    public int calculateScore(List<Map<String, Object>> accounts, boolean has2FA, boolean breachChecked) {
        int score = 0;
        int totalAccounts = accounts.size();

        if (totalAccounts == 0) return 0;

        // Base: accounts count
        score += Math.min(30, totalAccounts * 5);

        // 2FA
        if (has2FA) score += 30;

        // Breach check
        if (breachChecked) score += 20;

        // Each account found = +2
        for (Map<String, Object> acc : accounts) {
            if (Boolean.TRUE.equals(acc.get("found"))) score += 2;
        }

        return Math.min(100, score);
    }
}
