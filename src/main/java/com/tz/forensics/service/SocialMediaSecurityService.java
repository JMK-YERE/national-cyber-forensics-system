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
            "https://www.facebook.com/security/2fac/setup/intro/",
            new String[]{
                "Fungua Settings → Security and Login",
                "Bonyeza 'Two-Factor Authentication' → 'Use two-factor authentication'",
                "Chagua 'Authentication App' au 'Text message (SMS)'",
                "Angalia 'Where You're Logged In' → 'See More' → ondoa devices usizozijua",
                "Angalia 'Apps and Websites' → ondoa apps zisizotumika",
                "Weka 'Get alerts about unrecognized logins'"
            },
            "https://www.facebook.com/settings?tab=security"
        ));

        platforms.add(createPlatform(
            "instagram", "Instagram", "📷",
            "https://www.instagram.com/accounts/two_factor_authentication/",
            "https://www.instagram.com/accounts/activity/",
            new String[]{
                "Fungua Profile → Menu (≡) → Settings → Security",
                "Bonyeza 'Two-Factor Authentication' → 'Get Started'",
                "Chagua 'Authentication App' au 'Text Message'",
                "Angalia 'Login Activity' — ondoa devices usizozijua",
                "Weka 'Emails and Phone Number' kwa recovery",
                "Angalia 'Apps and Websites' — ondoa zisizotumika"
            },
            "https://www.instagram.com/accounts/two_factor_authentication/"
        ));

        platforms.add(createPlatform(
            "tiktok", "TikTok", "🎵",
            "https://www.tiktok.com/setting/security",
            "https://www.tiktok.com/setting/security",
            new String[]{
                "Fungua Profile → Menu (☰) → Settings and privacy",
                "Bonyeza 'Security' → '2-step verification'",
                "Chagua 'Authenticator app' au 'SMS'",
                "Angalia 'Manage Devices' — ondoa devices usizozijua",
                "Weka 'Login Alerts' kama inapatikana",
                "Fanya account yako 'Private' kama unataka"
            },
            "https://www.tiktok.com/setting/security"
        ));

        platforms.add(createPlatform(
            "whatsapp", "WhatsApp", "💬",
            "https://faq.whatsapp.com/571113305885923",
            "https://faq.whatsapp.com/571113305885923",
            new String[]{
                "Fungua WhatsApp → Menu (⋮) → Settings → Account",
                "Bonyeza 'Two-step verification' → 'Enable'",
                "Weka PIN ya tarakimu 6 (usisahau!)",
                "Weka email ya recovery",
                "Fungua 'Privacy' → weka 'Fingerprint Lock' (kama inapatikana)",
                "Angalia 'Linked Devices' — ondoa devices usizozijua"
            },
            "https://faq.whatsapp.com/571113305885923"
        ));

        platforms.add(createPlatform(
            "x", "X (Twitter)", "🐦",
            "https://twitter.com/settings/security",
            "https://twitter.com/settings/security",
            new String[]{
                "Fungua Menu → Settings and Support → Settings and privacy",
                "Bonyeza 'Security and account access' → 'Security'",
                "Weka 'Two-factor authentication' kwa 'Authentication app'",
                "Angalia 'Apps and sessions' → ondoa zisizotumika",
                "Weka 'Password reset protection'",
                "Angalia 'Login history' — ondoa sessions usizozijua"
            },
            "https://twitter.com/settings/security"
        ));

        platforms.add(createPlatform(
            "gmail", "Gmail (Google)", "📧",
            "https://myaccount.google.com/security",
            "https://myaccount.google.com/signinoptions/twosv",
            new String[]{
                "Fungua https://myaccount.google.com/security",
                "Bonyeza '2-Step Verification' → 'Get started'",
                "Weka phone number yako kwa SMS au 'Authenticator app'",
                "Angalia 'Your devices' — ondoa devices usizozijua",
                "Angalia 'Third-party apps with account access' — ondoa zisizotumika",
                "Fanya 'Security Checkup' kila miezi 3"
            },
            "https://myaccount.google.com/security"
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
                                                String[] steps, String directLink) {
        Map<String, String> p = new HashMap<>();
        p.put("id", id);
        p.put("name", name);
        p.put("icon", icon);
        p.put("settingsUrl", settingsUrl);
        p.put("twoFactorUrl", twoFactorUrl);
        p.put("steps", String.join("|||", steps));
        p.put("directLink", directLink);
        return p;
    }
}
