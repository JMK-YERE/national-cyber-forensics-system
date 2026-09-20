package com.tz.forensics.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SocialMediaSecurityService {

    public List<Map<String, String>> getAllPlatforms() {
        List<Map<String, String>> platforms = new ArrayList<>();

        platforms.add(createPlatform(
            "facebook", "Facebook", "👥",
            "https://www.facebook.com/settings?tab=security",
            "https://www.facebook.com/settings?tab=security&section=two_factor",
            new String[]{
                "Nenda Security Settings",
                "Bonyeza 'Two-Factor Authentication' — weka kwa SMS au App",
                "Weka 'Login Alerts' ili upate taarifa",
                "Angalia 'Where You're Logged In' — ondoa devices usizozijua",
                "Angalia 'Apps and Websites' — ondoa zisizotumika"
            }
        ));

        platforms.add(createPlatform(
            "instagram", "Instagram", "📷",
            "https://www.instagram.com/accounts/two_factor_authentication/",
            "https://www.instagram.com/accounts/activity/",
            new String[]{
                "Fungua Settings → Security",
                "Weka 'Two-Factor Authentication' kwa App",
                "Angalia 'Login Activity' — ondoa devices usizozijua",
                "Weka 'Emails and Phone Number' kwa recovery",
                "Fanya account yako private kama unataka"
            }
        ));

        platforms.add(createPlatform(
            "tiktok", "TikTok", "🎵",
            "https://www.tiktok.com/setting/security",
            "https://www.tiktok.com/setting/security/password",
            new String[]{
                "Fungua Settings → Security",
                "Weka '2-Step Verification' kwa App",
                "Angalia 'Manage Devices' — ondoa devices usizozijua",
                "Usibonyeze links kutoka DM za kutiliwa shaka",
                "Fanya account yako private"
            }
        ));

        platforms.add(createPlatform(
            "whatsapp", "WhatsApp", "💬",
            "https://faq.whatsapp.com/489853655551903",
            "https://faq.whatsapp.com/571113305885923",
            new String[]{
                "Fungua WhatsApp → Settings → Account",
                "Bonyeza 'Two-Step Verification' — weka PIN ya tarakimu 6",
                "Weka 'Fingerprint Lock' (kama inapatikana)",
                "Angalia 'Linked Devices' — ondoa zisizotumika",
                "Weka 'Privacy' → 'Last Seen' kuwa 'My Contacts'"
            }
        ));

        platforms.add(createPlatform(
            "x", "X (Twitter)", "🐦",
            "https://twitter.com/settings/security",
            "https://twitter.com/settings/sessions",
            new String[]{
                "Fungua Settings → Security and account access",
                "Weka 'Two-Factor Authentication' kwa App",
                "Angalia 'Apps and sessions' — ondoa zisizotumika",
                "Weka 'Password reset protection'",
                "Angalia 'Login history'"
            }
        ));

        platforms.add(createPlatform(
            "gmail", "Gmail (Google)", "📧",
            "https://myaccount.google.com/security",
            "https://myaccount.google.com/device-activity",
            new String[]{
                "Fungua Google Account → Security",
                "Weka '2-Step Verification' (kwa App)",
                "Angalia 'Your devices' — ondoa devices usizozijua",
                "Angalia 'Third-party apps' — ondoa zisizotumika",
                "Weka 'Recovery email' na 'Recovery phone'",
                "Fanya 'Security Checkup' kila miezi 3"
            }
        ));

        return platforms;
    }

    public Map<String, String> getPlatformInfo(String platformId) {
        for (Map<String, String> p : getAllPlatforms()) {
            if (p.get("id").equals(platformId)) return p;
        }
        return null;
    }

    private Map<String, String> createPlatform(String id, String name, String icon,
                                                String settingsUrl, String twoFactorUrl,
                                                String[] steps) {
        Map<String, String> p = new HashMap<>();
        p.put("id", id);
        p.put("name", name);
        p.put("icon", icon);
        p.put("settingsUrl", settingsUrl);
        p.put("twoFactorUrl", twoFactorUrl);
        p.put("steps", String.join("|||", steps));
        return p;
    }
}
