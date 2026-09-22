package com.tz.forensics.service;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.repository.ReportAttackRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class ReportAttackService {

    private final ReportAttackRepository repo;
    private static final Random random = new Random();

    public ReportAttackService(ReportAttackRepository repo) {
        this.repo = repo;
    }

    public ReportAttack create(ReportAttack report) {
        String reportId = "ATK-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + (100 + random.nextInt(900));
        report.setReportId(reportId);
        report.setStatus("NEW");
        report.setPriority(determinePriority(report.getAttackType()));
        report.setAttackCategory(determineCategory(report.getAttackType()));
        report.setAiRecommendation(getAIRecommendation(report.getAttackType()));

        // Set coordinates from region (auto)
        if (report.getRegion() != null && report.getLatitude() == null) {
            double[] coords = getRegionCoords(report.getRegion());
            if (coords != null) {
                report.setLatitude(coords[0]);
                report.setLongitude(coords[1]);
            }
        }

        return repo.save(report);
    }

    public List<ReportAttack> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public List<ReportAttack> getMine(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<ReportAttack> getRecent24h() {
        return repo.findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime.now().minusHours(24));
    }

    public ReportAttack getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void updateStatus(Long id, String status, String response, Long assignedTo, String assignedToName) {
        ReportAttack r = repo.findById(id).orElse(null);
        if (r != null) {
            r.setStatus(status);
            if (response != null) r.setAdminResponse(response);
            if (assignedTo != null) r.setAssignedTo(assignedTo);
            if (assignedToName != null) r.setAssignedToName(assignedToName);
            r.setUpdatedAt(LocalDateTime.now());
            repo.save(r);
        }
    }

    public long countNew() { return repo.countByStatus("NEW"); }
    public long countToday() { return repo.countByCreatedAtAfter(LocalDateTime.now().minusDays(1)); }
    public long countTotal() { return repo.count(); }

    private String determinePriority(String type) {
        if (type == null) return "MEDIUM";
        return switch (type) {
            case "PHONE_STOLEN", "MONEY_STOLEN", "BANK_CARD_LOST", "HOME_BREAK_IN" -> "HIGH";
            case "SOCIAL_MEDIA_HACKED", "EMAIL_HACKED", "IDENTITY_THEFT", "SIM_SWAP" -> "HIGH";
            case "RANSOMWARE" -> "CRITICAL";
            default -> "MEDIUM";
        };
    }

    private String determineCategory(String type) {
        if (type == null) return "OTHER";
        return switch (type) {
            case "PHONE_STOLEN", "HOME_BREAK_IN", "DOCUMENTS_STOLEN" -> "PHYSICAL";
            case "SOCIAL_MEDIA_HACKED", "EMAIL_HACKED", "PHISHING", "IDENTITY_THEFT", "SIM_SWAP", "RANSOMWARE" -> "DIGITAL";
            case "BANK_CARD_LOST", "MONEY_STOLEN" -> "FINANCIAL";
            default -> "OTHER";
        };
    }

    public String getAIRecommendation(String type) {
        if (type == null) return "Wasiliana na Polisi (112) na benki yako mara moja.";
        return switch (type) {
            case "PHONE_STOLEN" -> "📱 HATUA ZA HARAKA:\n\n1️⃣ Fanya simu yako 'locked' — piga *XXX#\n2️⃣ Pata OB number kutoka Polisi (112)\n3️⃣ Badilisha password za Gmail, FB, IG, WhatsApp\n4️⃣ Futa WhatsApp kwa 'Find My Device'\n5️⃣ Zuia SIM kwa kampuni yako ya mawasiliano\n6️⃣ Backup contacts kwenye cloud";
            case "SOCIAL_MEDIA_HACKED" -> "👤 HATUA ZA HARAKA:\n\n1️⃣ Tumia 'Forgot Password' kwenye platform\n2️⃣ Wasiliana na Support mara moja\n3️⃣ Waambie marafiki kuhusu hack\n4️⃣ Weka 2FA kwenye accounts zote\n5️⃣ Badilisha password ya email kwanza\n6️⃣ Angalia login activity";
            case "BANK_CARD_LOST" -> "💳 HATUA ZA HARAKA:\n\n1️⃣ Piga simu benki — ZUIA kadi!\n2️⃣ Wasiliana na Polisi (112)\n3️⃣ Angalia transactions zote\n4️⃣ Omba kadi mpya\n5️⃣ Badilisha PIN na mobile banking password";
            case "MONEY_STOLEN" -> "💰 HATUA ZA HARAKA:\n\n1️⃣ Wasiliana na benki/wakala mara moja\n2️⃣ Pata OB number kutoka Polisi\n3️⃣ Angalia transaction history\n4️⃣ Weka 2FA kwenye mobile banking\n5️⃣ Taarifu TCRA kama ni mtandao\n6️⃣ Backup statements zote";
            case "EMAIL_HACKED" -> "📧 HATUA ZA HARAKA:\n\n1️⃣ Badilisha password mara moja\n2️⃣ Angalia 'Recent Security Activity'\n3️⃣ Futa apps zisizotumika\n4️⃣ Weka 2FA\n5️⃣ Waambie contacts zako\n6️⃣ Check HaveIBeenPwned";
            case "PHISHING" -> "🎣 HATUA ZA HARAKA:\n\n1️⃣ Usibonyeze links tena\n2️⃣ Badilisha password kama umeshaitumia\n3️⃣ Angalia account kama imeingiliwa\n4️⃣ Ripoti kwa platform\n5️⃣ Weka 2FA";
            case "HOME_BREAK_IN" -> "🏠 HATUA ZA HARAKA:\n\n1️⃣ Piga simu 112/999 (Polisi) mara moja\n2️⃣ Usiguse kitu chochote (evidence)\n3️⃣ Piga picha kama ni salama\n4️⃣ Andika kila kitu kilichoibiwa\n5️⃣ Wasiliana na bima\n6️⃣ Badilisha locks zote";
            case "IDENTITY_THEFT" -> "🆔 HATUA ZA HARAKA:\n\n1️⃣ Ripoti Polisi mara moja\n2️⃣ Wasiliana na benki zote\n3️⃣ Weka Fraud Alert\n4️⃣ Angalia credit report\n5️⃣ Badilisha password zote\n6️⃣ Ripoti TCRA";
            case "SIM_SWAP" -> "📲 HATUA ZA HARAKA:\n\n1️⃣ Piga simu kampuni yako — ZUIA SIM\n2️⃣ Badilisha passwords zote\n3️⃣ Angalia accounts za benki\n4️⃣ Ripoti Polisi\n5️⃣ Weka 2FA (App, sio SMS)";
            case "RANSOMWARE" -> "🦠 HATUA ZA HARAKA:\n\n1️⃣ USILIpe fidia\n2️⃣ KATUA mtandao\n3️⃣ Ripoti Polisi (112)\n4️⃣ Wasiliana IT team\n5️⃣ Backup data muhimu\n6️⃣ Fanya scan ya virusi";
            default -> "⚠️ HATUA ZA HARAKA:\n\n1️⃣ Andika kila kitu kilichotokea\n2️⃣ Ripoti Polisi (112/999)\n3️⃣ Taarifu benki/taasisi husika\n4️⃣ Badilisha password zote\n5️⃣ Weka 2FA\n6️⃣ Backup documents";
        };
    }

    private double[] getRegionCoords(String region) {
        if (region == null) return null;
        return switch (region) {
            case "Dar es Salaam" -> new double[]{-6.7924, 39.2083};
            case "Arusha" -> new double[]{-3.3869, 36.6830};
            case "Mwanza" -> new double[]{-2.5164, 32.9175};
            case "Dodoma" -> new double[]{-6.1630, 35.7516};
            case "Mbeya" -> new double[]{-8.9094, 33.4608};
            case "Morogoro" -> new double[]{-6.8278, 37.6591};
            case "Tanga" -> new double[]{-5.0689, 39.0988};
            case "Zanzibar" -> new double[]{-6.1659, 39.2026};
            case "Kilimanjaro" -> new double[]{-3.3471, 37.3422};
            case "Tabora" -> new double[]{-5.0167, 32.8000};
            case "Kigoma" -> new double[]{-4.8769, 29.6269};
            case "Iringa" -> new double[]{-7.7700, 35.6900};
            case "Mtwara" -> new double[]{-10.2667, 40.1833};
            case "Ruvuma" -> new double[]{-10.6833, 35.6500};
            case "Singida" -> new double[]{-4.8167, 34.7500};
            case "Shinyanga" -> new double[]{-3.6619, 33.4212};
            case "Kagera" -> new double[]{-1.8825, 31.3900};
            case "Mara" -> new double[]{-1.7750, 34.1500};
            case "Manyara" -> new double[]{-4.3167, 36.0667};
            case "Rukwa" -> new double[]{-8.0000, 31.5000};
            case "Katavi" -> new double[]{-6.3667, 31.0500};
            case "Njombe" -> new double[]{-9.3333, 34.7667};
            case "Simiyu" -> new double[]{-2.8333, 33.9833};
            case "Geita" -> new double[]{-2.8667, 32.1667};
            case "Songwe" -> new double[]{-8.8514, 32.8925};
            case "Pwani" -> new double[]{-6.9333, 38.9167};
            case "Lindi" -> new double[]{-10.0000, 39.7167};
            default -> null;
        };
    }
}
