package com.tz.forensics.service;

import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.repository.IncidentRepository;
import com.tz.forensics.repository.ReportAttackRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIThreatDetectionService {

    private final IncidentRepository incidentRepo;
    private final ReportAttackRepository attackRepo;
    private final AIChatService aiChatService;

    public AIThreatDetectionService(IncidentRepository incidentRepo,
                                     ReportAttackRepository attackRepo,
                                     AIChatService aiChatService) {
        this.incidentRepo = incidentRepo;
        this.attackRepo = attackRepo;
        this.aiChatService = aiChatService;
    }

    // ===== THREAT ANALYSIS =====
    public Map<String, Object> analyzeThreats() {
        Map<String, Object> result = new HashMap<>();

        List<Incident> incidents = incidentRepo.findAll();
        List<ReportAttack> attacks = attackRepo.findAll();

        // Total counts
        result.put("totalIncidents", incidents.size());
        result.put("totalAttacks", attacks.size());

        // Last 24h
        LocalDateTime dayAgo = LocalDateTime.now().minusHours(24);
        long recentIncidents = incidents.stream()
                .filter(i -> i.getDateReported() != null && i.getDateReported().isAfter(dayAgo))
                .count();
        long recentAttacks = attacks.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(dayAgo))
                .count();
        result.put("recentIncidents", recentIncidents);
        result.put("recentAttacks", recentAttacks);

        // Top attack types
        Map<String, Long> attackTypes = attacks.stream()
                .filter(a -> a.getAttackType() != null)
                .collect(Collectors.groupingBy(ReportAttack::getAttackType, Collectors.counting()));
        result.put("attackTypes", attackTypes);

        // Top regions
        Map<String, Long> topRegions = attacks.stream()
                .filter(a -> a.getRegion() != null)
                .collect(Collectors.groupingBy(ReportAttack::getRegion, Collectors.counting()));

        List<Map.Entry<String, Long>> topRegionsList = topRegions.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());
        result.put("topRegions", topRegionsList);

        // Risk score
        int riskScore = calculateRiskScore(recentIncidents + recentAttacks, attacks.size());
        result.put("riskScore", riskScore);
        result.put("riskLevel", riskScore >= 70 ? "CRITICAL" : riskScore >= 50 ? "HIGH" : riskScore >= 30 ? "MEDIUM" : "LOW");

        // AI Analysis
        String aiAnalysis = getAIAnalysis(recentIncidents, recentAttacks, attackTypes, topRegionsList);
        result.put("aiAnalysis", aiAnalysis);

        return result;
    }

    // ===== PREDICT THREATS =====
    public Map<String, Object> predictThreats() {
        Map<String, Object> result = new HashMap<>();

        List<ReportAttack> attacks = attackRepo.findAll();

        // Analyze last 7 days vs previous 7 days
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusDays(14);

        long thisWeek = attacks.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(weekAgo))
                .count();
        long lastWeek = attacks.stream()
                .filter(a -> a.getCreatedAt() != null &&
                        a.getCreatedAt().isAfter(twoWeeksAgo) &&
                        a.getCreatedAt().isBefore(weekAgo))
                .count();

        result.put("thisWeek", thisWeek);
        result.put("lastWeek", lastWeek);

        double trend = lastWeek > 0 ? ((double)(thisWeek - lastWeek) / lastWeek) * 100 : 0;
        result.put("trend", Math.round(trend));
        result.put("trendDirection", trend > 0 ? "INCREASING" : trend < 0 ? "DECREASING" : "STABLE");

        // Predict next week
        long predicted = Math.round(thisWeek * (1 + trend / 100));
        result.put("predictedNextWeek", Math.max(0, predicted));

        // Hotspots (regions with most attacks)
        Map<String, Long> regionCounts = attacks.stream()
                .filter(a -> a.getRegion() != null && a.getCreatedAt() != null && a.getCreatedAt().isAfter(twoWeeksAgo))
                .collect(Collectors.groupingBy(ReportAttack::getRegion, Collectors.counting()));

        List<Map.Entry<String, Long>> hotspots = regionCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());
        result.put("hotspots", hotspots);

        // AI prediction text
        String prediction = getAIPrediction(thisWeek, lastWeek, trend, hotspots);
        result.put("aiPrediction", prediction);

        return result;
    }

    // ===== ANALYZE SINGLE INCIDENT =====
    public Map<String, Object> analyzeIncident(String title, String description) {
        Map<String, Object> result = new HashMap<>();

        // Simple keyword-based classification
        String lower = (title + " " + description).toLowerCase();

        String category = "OTHER";
        String severity = "MEDIUM";
        String mitreTactic = "Unknown";
        int cvss = 5;

        if (lower.contains("phishing") || lower.contains("email") || lower.contains("link")) {
            category = "Phishing";
            severity = "HIGH";
            mitreTactic = "Initial Access";
            cvss = 7;
        } else if (lower.contains("ransomware") || lower.contains("encrypt")) {
            category = "Ransomware";
            severity = "CRITICAL";
            mitreTactic = "Impact";
            cvss = 9;
        } else if (lower.contains("data breach") || lower.contains("database") || lower.contains("leak")) {
            category = "Data Breach";
            severity = "CRITICAL";
            mitreTactic = "Exfiltration";
            cvss = 9;
        } else if (lower.contains("ddos") || lower.contains("flood")) {
            category = "DDoS";
            severity = "HIGH";
            mitreTactic = "Impact";
            cvss = 7;
        } else if (lower.contains("malware") || lower.contains("virus")) {
            category = "Malware";
            severity = "HIGH";
            mitreTactic = "Execution";
            cvss = 8;
        } else if (lower.contains("hack") || lower.contains("unauthorized")) {
            category = "Unauthorized Access";
            severity = "HIGH";
            mitreTactic = "Credential Access";
            cvss = 8;
        } else if (lower.contains("social media") || lower.contains("facebook") || lower.contains("instagram")) {
            category = "Social Media Hacking";
            severity = "HIGH";
            mitreTactic = "Credential Access";
            cvss = 7;
        } else if (lower.contains("sim swap") || lower.contains("sim")) {
            category = "SIM Swap";
            severity = "HIGH";
            mitreTactic = "Credential Access";
            cvss = 8;
        }

        result.put("category", category);
        result.put("severity", severity);
        result.put("mitreTactic", mitreTactic);
        result.put("cvssScore", cvss);
        result.put("recommendation", getRecommendation(category));

        return result;
    }

    private int calculateRiskScore(long recentTotal, long allTime) {
        int score = 0;

        // Recent activity
        if (recentTotal > 20) score += 40;
        else if (recentTotal > 10) score += 30;
        else if (recentTotal > 5) score += 20;
        else if (recentTotal > 0) score += 10;

        // All-time
        if (allTime > 100) score += 30;
        else if (allTime > 50) score += 20;
        else if (allTime > 20) score += 10;

        // Time factor (mchana = hatari zaidi)
        int hour = LocalDateTime.now().getHour();
        if (hour >= 8 && hour <= 18) score += 10;

        return Math.min(100, score);
    }

    private String getAIAnalysis(long recentInc, long recentAtk,
                                   Map<String, Long> types,
                                   List<Map.Entry<String, Long>> regions) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 UCHAMBUZI WA HATARI:\n\n");

        long total = recentInc + recentAtk;
        if (total > 20) {
            sb.append("🔴 Hatari ni KUBWA sana! Kumekuwa na matukio ").append(total).append(" katika saa 24 zilizopita.\n\n");
        } else if (total > 10) {
            sb.append("🟠 Hatari ni YA KATI. Matukio ").append(total).append(" katika saa 24 zilizopita.\n\n");
        } else if (total > 0) {
            sb.append("🟡 Hatari ni NDOGO. Matukio ").append(total).append(" katika saa 24 zilizopita.\n\n");
        } else {
            sb.append("🟢 Hakuna matukio mapya katika saa 24 zilizopita.\n\n");
        }

        if (!types.isEmpty()) {
            sb.append("🎯 Aina za mashambulizi:\n");
            types.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .forEach(e -> sb.append("   • ").append(e.getKey()).append(": ").append(e.getValue()).append("\n"));
        }

        if (!regions.isEmpty()) {
            sb.append("\n📍 Mikoa yenye matukio mengi:\n");
            for (Map.Entry<String, Long> r : regions) {
                sb.append("   • ").append(r.getKey()).append(": ").append(r.getValue()).append("\n");
            }
        }

        sb.append("\n💡 Mapendekezo:\n");
        sb.append("   ✅ Weka 2FA kwenye accounts zote\n");
        sb.append("   ✅ Badilisha passwords\n");
        sb.append("   ✅ Ripoti matukio mara moja");

        return sb.toString();
    }

    private String getAIPrediction(long thisWeek, long lastWeek, double trend,
                                     List<Map.Entry<String, Long>> hotspots) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔮 Utabiri wa Wiki Ijayo:\n\n");
        sb.append("📈 Wiki hii: ").append(thisWeek).append(" matukio\n");
        sb.append("📉 Wiki iliyopita: ").append(lastWeek).append(" matukio\n");

        if (trend > 20) {
            sb.append("\n🚨 ONA: Matukio yanaongezeka kwa ").append(Math.round(trend)).append("%!\n");
            sb.append("📢 Mapendekezo:\n");
            sb.append("   • Ongeza ulinzi\n");
            sb.append("   • Weka alerts\n");
            sb.append("   • Elimisha watu");
        } else if (trend < -20) {
            sb.append("\n✅ VIZURI: Matukio yanapungua kwa ").append(Math.abs(Math.round(trend))).append("%.\n");
            sb.append("💡 Endelea kufuatilia.");
        } else {
            sb.append("\n➡️ Matukio ni sawa (stable).\n");
            sb.append("💡 Endelea kufuatilia.");
        }

        if (!hotspots.isEmpty()) {
            sb.append("\n\n🎯 Mikoa yenye hatari:\n");
            for (Map.Entry<String, Long> h : hotspots) {
                sb.append("   ⚠️ ").append(h.getKey()).append(" (").append(h.getValue()).append(" matukio)\n");
            }
        }

        return sb.toString();
    }

    private String getRecommendation(String category) {
        return switch (category) {
            case "Phishing" -> "🎣 Phishing: Angalia URL, usibonyeze links, weka 2FA";
            case "Ransomware" -> "🦠 Ransomware: Backup data, usilipe fidia, ripoti Polisi";
            case "Data Breach" -> "💾 Data Breach: Badilisha passwords, wasiliana na benki";
            case "DDoS" -> "🌊 DDoS: Wasiliana na ISP, weka firewall";
            case "Malware" -> "🦠 Malware: Fanya scan, futa virus, update OS";
            case "Unauthorized Access" -> "🔓 Unauthorized: Badilisha passwords, weka 2FA";
            case "Social Media Hacking" -> "📱 Social Media: Tumia 'Forgot Password', wasiliana Support";
            case "SIM Swap" -> "📲 SIM Swap: Zuia SIM, badilisha passwords zote";
            default -> "✅ Ripoti Polisi (112), badilisha passwords zote";
        };
    }
}
