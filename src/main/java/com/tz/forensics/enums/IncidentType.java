package com.tz.forensics.enums;

public enum IncidentType {

    PHISHING("🎣 Phishing", "🎣", "#f59e0b"),
    RANSOMWARE("🔒 Ransomware", "🔒", "#dc2626"),
    HACKING("💻 Hacking", "💻", "#dc2626"),
    FRAUD("💰 Fraud", "💰", "#eab308"),
    CYBERBULLYING("😢 Cyberbullying", "😢", "#a855f7"),
    DATA_BREACH("📊 Data Breach", "📊", "#3b82f6"),
    SIM_SWAP("📱 SIM Swap", "📱", "#10b981"),
    IDENTITY_THEFT("🆔 Identity Theft", "🆔", "#a855f7"),
    ONLINE_SCAM("🕸️ Online Scam", "🕸️", "#f59e0b"),
    REVENGE_PORN("🚫 Revenge Porn", "🚫", "#dc2626"),
    OTHER("❓ Other", "❓", "#6b7280");

    private final String label;
    private final String icon;
    private final String color;

    IncidentType(String label, String icon, String color) {
        this.label = label;
        this.icon = icon;
        this.color = color;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }
}
