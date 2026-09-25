package com.tz.forensics.config;

import com.tz.forensics.enums.IncidentType;

import java.util.*;

public class DynamicFieldConfig {

    public static class Field {
        public String name;
        public String label;
        public String type;       // text, textarea, number, date, select, boolean
        public boolean required;
        public List<String> options;

        public Field(String name, String label, String type, boolean required) {
            this.name = name; this.label = label; this.type = type;
            this.required = required; this.options = null;
        }

        public Field(String name, String label, String type, boolean required, List<String> options) {
            this.name = name; this.label = label; this.type = type;
            this.required = required; this.options = options;
        }
    }

    private static final Map<IncidentType, List<Field>> FIELDS = new LinkedHashMap<>();

    static {
        // ===== PHISHING =====
        FIELDS.put(IncidentType.PHISHING, List.of(
            new Field("phishing_url", "URL ya Uongo", "text", true),
            new Field("sender_email", "Barua Pepe ya Mtumaji", "text", true),
            new Field("message_content", "Ujumbe Uliosema", "textarea", true),
            new Field("user_replied", "Umejibu?", "boolean", false),
            new Field("user_shared_info", "Umetoa Taarifa Gani?", "text", false)
        ));

        // ===== RANSOMWARE =====
        FIELDS.put(IncidentType.RANSOMWARE, List.of(
            new Field("affected_files", "Faili Zilizoathirika", "textarea", true),
            new Field("ransom_note", "Ransom Note", "textarea", true),
            new Field("crypto_address", "Crypto Address", "text", false),
            new Field("attack_date", "Tarehe ya Mashambulizi", "date", true),
            new Field("backup_available", "Backup Ipo?", "boolean", false)
        ));

        // ===== HACKING =====
        FIELDS.put(IncidentType.HACKING, List.of(
            new Field("affected_account", "Akaunti Iliyoathirika", "text", true),
            new Field("discovery_date", "Tarehe ya Kugundua", "date", true),
            new Field("entry_method", "Njia ya Kuingia", "text", false),
            new Field("changes_made", "Mabadiliko Yaliyofanyika", "textarea", false),
            new Field("had_2fa", "2FA Ilikuwa?", "boolean", false)
        ));

        // ===== FRAUD =====
        FIELDS.put(IncidentType.FRAUD, List.of(
            new Field("amount_lost", "Kiasi Kilichoibiwa (TZS)", "number", true),
            new Field("payment_method", "Njia ya Malipo", "select", true,
                List.of("Mobile Money", "Bank Transfer", "Cash", "Card", "Crypto", "Other")),
            new Field("bank_name", "Benki/Mobile Money", "text", false),
            new Field("suspect_info", "Namba ya Mhusika", "text", false),
            new Field("communication_evidence", "Ushahidi wa Mawasiliano", "textarea", false)
        ));

        // ===== CYBERBULLYING =====
        FIELDS.put(IncidentType.CYBERBULLYING, List.of(
            new Field("platform", "Jukwaa", "select", true,
                List.of("Facebook", "WhatsApp", "Instagram", "Twitter/X", "TikTok", "Telegram", "Other")),
            new Field("bully_account", "Akaunti ya Mshambuliaji", "text", true),
            new Field("screenshots", "Screenshots", "textarea", false),
            new Field("incident_date", "Tarehe", "date", true)
        ));

        // ===== DATA BREACH =====
        FIELDS.put(IncidentType.DATA_BREACH, List.of(
            new Field("data_type", "Aina ya Data", "textarea", true),
            new Field("people_affected", "Idadi ya Watu", "number", false),
            new Field("discovery_method", "Njia ya Kugundua", "text", false),
            new Field("organization", "Taasisi Iliyoathirika", "text", true)
        ));

        // ===== SIM SWAP =====
        FIELDS.put(IncidentType.SIM_SWAP, List.of(
            new Field("phone_number", "Namba ya Simu", "text", true),
            new Field("network", "Mtandao", "select", true,
                List.of("Vodacom", "Airtel", "Tigo", "Halotel", "TTCL", "Other")),
            new Field("swap_date", "Tarehe ya Swap", "date", true),
            new Field("new_sim_number", "Namba Mpya", "text", false),
            new Field("blocked_attempted", "Umejaribu ku-block?", "boolean", false)
        ));

        // ===== IDENTITY THEFT =====
        FIELDS.put(IncidentType.IDENTITY_THEFT, List.of(
            new Field("id_type", "Aina ya ID", "select", true,
                List.of("NIDA", "Driving License", "Passport", "Voter ID", "Other")),
            new Field("id_number", "Namba ya ID", "text", true),
            new Field("unauthorized_use", "Matumizi Yasiyoidhinishwa", "textarea", true),
            new Field("incident_date", "Tarehe", "date", true)
        ));

        // ===== ONLINE SCAM =====
        FIELDS.put(IncidentType.ONLINE_SCAM, List.of(
            new Field("platform", "Jukwaa", "select", true,
                List.of("Facebook", "Instagram", "WhatsApp", "Telegram", "Twitter/X", "Other")),
            new Field("suspect_name", "Jina la Muhusika", "text", true),
            new Field("amount", "Kiasi (TZS)", "number", false),
            new Field("payment_method", "Njia ya Malipo", "text", false),
            new Field("evidence", "Ushahidi", "textarea", false)
        ));

        // ===== REVENGE PORN =====
        FIELDS.put(IncidentType.REVENGE_PORN, List.of(
            new Field("platform", "Jukwaa", "select", true,
                List.of("Facebook", "Instagram", "WhatsApp", "Telegram", "Twitter/X", "TikTok", "Other")),
            new Field("perpetrator_account", "Akaunti ya Mshambuliaji", "text", true),
            new Field("content_type", "Aina ya Content", "select", true,
                List.of("Photos", "Videos", "Both", "Threats")),
            new Field("incident_date", "Tarehe", "date", true),
            new Field("reported_platform", "Umejaribu ku-report?", "boolean", false)
        ));

        // ===== OTHER =====
        FIELDS.put(IncidentType.OTHER, List.of(
            new Field("details", "Maelezo ya Ziada", "textarea", true)
        ));
    }

    public static List<Field> getFields(IncidentType type) {
        return FIELDS.getOrDefault(type, Collections.emptyList());
    }

    public static Map<IncidentType, List<Field>> getAllFields() {
        return FIELDS;
    }
}
