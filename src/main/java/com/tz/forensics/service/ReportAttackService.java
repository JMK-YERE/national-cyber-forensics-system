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
        return repo.save(report);
    }

    public ReportAttack save(ReportAttack report) {
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
            if (response != null && !response.isEmpty()) r.setAdminResponse(response);
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
            case "PHONE_STOLEN", "MONEY_STOLEN", "BANK_CARD_LOST", "HOME_BREAK_IN", "RANSOMWARE" -> "HIGH";
            case "SOCIAL_MEDIA_HACKED", "EMAIL_HACKED", "IDENTITY_THEFT", "SIM_SWAP" -> "HIGH";
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
        if (type == null) return "Wasiliana na Polisi mara moja.";
        return switch (type) {
            case "PHONE_STOLEN" -> "📱 HATUA ZA HARAKA:\n\n1️⃣ Fanya simu yako 'locked'\n2️⃣ Pata OB number kutoka Polisi\n3️⃣ Badilisha password za accounts zote\n4️⃣ Futa WhatsApp kwa 'Find My Device'\n5️⃣ Zuia SIM kwa kampuni yako\n6️⃣ Backup contacts";
            case "SOCIAL_MEDIA_HACKED" -> "👤 HATUA ZA HARAKA:\n\n1️⃣ Tumia 'Forgot Password'\n2️⃣ Wasiliana na Support mara moja\n3️⃣ Waambie marafiki\n4️⃣ Weka 2FA kwenye accounts zote\n5️⃣ Badilisha password ya email kwanza";
            case "BANK_CARD_LOST" -> "💳 HATUA ZA HARAKA:\n\n1️⃣ Piga simu benki — ZUIA kadi!\n2️⃣ Wasiliana na Polisi\n3️⃣ Angalia transactions zote\n4️⃣ Omba kadi mpya\n5️⃣ Badilisha PIN";
            case "MONEY_STOLEN" -> "💰 HATUA ZA HARAKA:\n\n1️⃣ Wasiliana na benki/wakala\n2️⃣ Pata OB number\n3️⃣ Angalia transaction history\n4️⃣ Weka 2FA kwenye mobile banking\n5️⃣ Taarifu regulator";
            case "EMAIL_HACKED" -> "📧 HATUA ZA HARAKA:\n\n1️⃣ Badilisha password mara moja\n2️⃣ Angalia 'Recent Security Activity'\n3️⃣ Futa apps zisizotumika\n4️⃣ Weka 2FA\n5️⃣ Waambie contacts zako";
            case "PHISHING" -> "🎣 HATUA ZA HARAKA:\n\n1️⃣ Usibonyeze links tena\n2️⃣ Badilisha password kama umeshaitumia\n3️⃣ Angalia account\n4️⃣ Ripoti kwa platform\n5️⃣ Weka 2FA";
            case "HOME_BREAK_IN" -> "🏠 HATUA ZA HARAKA:\n\n1️⃣ Piga simu Polisi mara moja\n2️⃣ Usiguse kitu chochote\n3️⃣ Piga picha\n4️⃣ Andika kilichoibiwa\n5️⃣ Wasiliana na bima";
            case "IDENTITY_THEFT" -> "🆔 HATUA ZA HARAKA:\n\n1️⃣ Ripoti Polisi\n2️⃣ Wasiliana na benki zote\n3️⃣ Weka Fraud Alert\n4️⃣ Angalia credit report\n5️⃣ Badilisha password zote";
            case "SIM_SWAP" -> "📲 HATUA ZA HARAKA:\n\n1️⃣ Piga simu kampuni — ZUIA SIM\n2️⃣ Badilisha passwords zote\n3️⃣ Angalia benki\n4️⃣ Ripoti Polisi\n5️⃣ Weka 2FA (App, sio SMS)";
            case "RANSOMWARE" -> "🦠 HATUA ZA HARAKA:\n\n1️⃣ USILIpe fidia\n2️⃣ KATUA mtandao\n3️⃣ Ripoti Polisi\n4️⃣ Wasiliana IT\n5️⃣ Backup data muhimu";
            default -> "⚠️ HATUA ZA HARAKA:\n\n1️⃣ Andika kila kitu\n2️⃣ Ripoti Polisi\n3️⃣ Taarifu benki\n4️⃣ Badilisha passwords\n5️⃣ Weka 2FA";
        };
    }
}
