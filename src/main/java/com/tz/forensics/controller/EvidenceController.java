package com.tz.forensics.controller;

import com.tz.forensics.dto.EvidenceDto;
import com.tz.forensics.entity.Evidence;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.service.CaseFileService;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.repository.IncidentRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.EvidenceService;
import com.tz.forensics.service.NotificationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Set;

@Controller
@RequestMapping("/evidence")
public class EvidenceController {
    private final EvidenceService evidenceService;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final CaseFileService caseFileService;
    private final NotificationService notificationService;

    public EvidenceController(EvidenceService evidenceService, AuditService auditService,
                              UserRepository userRepository, IncidentRepository incidentRepository,
                              CaseFileService caseFileService,
                              NotificationService notificationService) {
        this.evidenceService = evidenceService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
        this.caseFileService = caseFileService;
        this.notificationService = notificationService;
    }

    private User currentUser(Authentication auth) {
        return auth == null ? null : userRepository.findByUsername(auth.getName()).orElse(null);
    }

    private boolean isStaff(User user) {
        return user != null && (user.isAdmin() || user.isProfessional() || user.isForensics()
                || "ANALYST".equalsIgnoreCase(user.getRole()));
    }

    private boolean canAcquireEvidence(User user) {
        return user != null && (user.isAdmin() || user.isProfessional() || user.isForensics());
    }

    private boolean canAccessIncident(Incident incident, User user) {
        if (incident == null || user == null) return false;
        return user.isAdmin() || user.isProfessional() || user.isForensics()
                || (user.isAnalyst() && (user.getId().equals(incident.getAssignedTo())
                    || user.getId().equals(incident.getReporterUserId())));
    }

    private boolean canAccessCase(CaseFile caseFile, User user) {
        if (caseFile == null || user == null) return false;
        if (user.isAdmin() || user.isProfessional() || user.isForensics()) return true;
        return "ANALYST".equalsIgnoreCase(user.getRole())
                && (user.getId().equals(caseFile.getCreatedBy())
                    || user.getId().equals(caseFile.getAssignedTo())
                    || user.getId().equals(caseFile.getLeadInvestigator()));
    }

    private boolean canAccessEvidence(Evidence evidence, Incident incident, User user) {
        if (evidence == null || user == null || !isStaff(user) || !canAccessIncident(incident, user)) return false;
        if (evidence.getCaseId() != null) {
            CaseFile cf = caseFileService.getById(evidence.getCaseId());
            if (cf == null || !incident.getId().equals(cf.getIncidentId()) || !canAccessCase(cf, user)) return false;
        }
        return true;
    }

    @GetMapping("/upload/{incidentId}")
    public String uploadForm(@PathVariable Long incidentId,
                             @RequestParam(value = "caseId", required = false) Long caseId,
                             Authentication auth, Model model) {
        User user = currentUser(auth);
        Incident incident = incidentRepository.findById(incidentId).orElse(null);
        if (!canAcquireEvidence(user) || !canAccessIncident(incident, user)) return "redirect:/access-denied";
        CaseFile caseFile = null;
        if (caseId != null) {
            caseFile = caseFileService.getById(caseId);
            if (caseFile == null || !incidentId.equals(caseFile.getIncidentId()) || !canAccessCase(caseFile, user)) {
                return "redirect:/access-denied";
            }
        }
        model.addAttribute("incidentId", incidentId);
        model.addAttribute("caseId", caseId);
        model.addAttribute("caseFile", caseFile);
        model.addAttribute("evidence", new EvidenceDto());
        model.addAttribute("evidenceList", caseId == null
                ? evidenceService.getEvidenceByIncident(incidentId).stream().filter(e -> canAccessEvidence(e, incident, user)).toList()
                : evidenceService.getEvidenceByCase(caseId).stream().filter(e -> canAccessEvidence(e, incident, user)).toList());
        return "evidence-upload";
    }

    @PostMapping("/upload/{incidentId}")
    public String uploadEvidence(@PathVariable Long incidentId,
                                 @RequestParam(value = "caseId", required = false) Long caseId,
                                 @RequestParam("file") MultipartFile file,
                                 @ModelAttribute("evidence") EvidenceDto dto,
                                 Authentication auth, Model model) {
        User user = currentUser(auth);
        Incident incident = incidentRepository.findById(incidentId).orElse(null);
        if (!isStaff(user) || !canAccessIncident(incident, user)) return "redirect:/access-denied";
        CaseFile caseFile = null;
        if (caseId != null) {
            caseFile = caseFileService.getById(caseId);
            if (caseFile == null || !incidentId.equals(caseFile.getIncidentId()) || !canAccessCase(caseFile, user)) {
                return "redirect:/access-denied";
            }
            if ("CLOSED".equalsIgnoreCase(caseFile.getStatus()) || "ARCHIVED".equalsIgnoreCase(caseFile.getStatus())) {
                return "redirect:/access-denied";
            }
        }

        try {
            if (file == null || file.isEmpty()) throw new IllegalArgumentException("Chagua evidence file kwanza.");
            Evidence saved = evidenceService.uploadEvidence(incidentId, caseId, file, dto, auth.getName(), user.getId(), user.getRole());
            auditService.log("UPLOAD_EVIDENCE", "Evidence", String.valueOf(saved.getId()),
                    "Uploaded: " + saved.getOriginalFilename() + " | SHA-256: " + saved.getSha256Hash());
            model.addAttribute("success", "Evidence imepakiwa! SHA-256: " + saved.getSha256Hash());
        } catch (Exception e) {
            model.addAttribute("error", "Hitilafu: " + e.getMessage());
        }
        model.addAttribute("incidentId", incidentId);
        model.addAttribute("caseId", caseId);
        model.addAttribute("caseFile", caseFile);
        model.addAttribute("evidence", new EvidenceDto());
        model.addAttribute("evidenceList", caseId == null
                ? evidenceService.getEvidenceByIncident(incidentId).stream().filter(e -> canAccessEvidence(e, incident, user)).toList()
                : evidenceService.getEvidenceByCase(caseId).stream().filter(e -> canAccessEvidence(e, incident, user)).toList());
        return "evidence-upload";
    }

    @GetMapping("/custody/{id}")
    public String custody(@PathVariable Long id, Authentication auth, Model model) {
        User user = currentUser(auth);
        Evidence evidence = evidenceService.getById(id);
        if (evidence == null) return "redirect:/incidents";
        Incident incident = incidentRepository.findById(evidence.getIncidentId()).orElse(null);
        if (!canAccessEvidence(evidence, incident, user)) return "redirect:/access-denied";

        model.addAttribute("evidence", evidence);
        model.addAttribute("incident", incident);
        model.addAttribute("custodyEvents", evidenceService.getChainOfCustody(id));
        model.addAttribute("custodians", userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()) && u.isApproved()
                        && (u.isAdmin() || u.isProfessional() || u.isForensics() || u.isAnalyst())).toList());
        model.addAttribute("canOperateCustody", canAcquireEvidence(user)
                || (evidence.getCustodianId() != null && evidence.getCustodianId().equals(user.getId())));
        auditService.log("VIEW_CHAIN_OF_CUSTODY", "Evidence", String.valueOf(id),
                "Viewed chain of custody | SHA-256: " + evidence.getSha256Hash());
        return "evidence-custody";
    }

    @PostMapping("/custody/{id}/transfer")
    public String transferCustody(@PathVariable Long id,@RequestParam("recipientId") Long recipientId,
                                  @RequestParam(value="purpose",required=false) String purpose,
                                  @RequestParam(value="notes",required=false) String notes,Authentication auth) {
        User actor=currentUser(auth); Evidence evidence=evidenceService.getById(id);
        if(evidence==null)return "redirect:/incidents";
        Incident incident=incidentRepository.findById(evidence.getIncidentId()).orElse(null);
        if(!canAccessEvidence(evidence,incident,actor))return "redirect:/access-denied";
        User recipient=userRepository.findById(recipientId).orElse(null);
        if(recipient==null||!Boolean.TRUE.equals(recipient.getEnabled())||!recipient.isApproved()
                ||(!recipient.isAdmin()&&!recipient.isProfessional()&&!recipient.isForensics()&&!recipient.isAnalyst())){
            auditService.log("REJECT_TRANSFER_EVIDENCE","Evidence",String.valueOf(id),"Invalid or inactive recipient: "+recipientId);
            return "redirect:/access-denied";
        }
        boolean ok=evidenceService.transferCustody(id,actor.getId(),auth.getName(),actor.getRole(),recipient.getId(),
                recipient.getFullName()==null?recipient.getUsername():recipient.getFullName(),recipient.getRole(),purpose,notes);
        auditService.log(ok?"TRANSFER_EVIDENCE":"FAILED_TRANSFER_EVIDENCE","Evidence",String.valueOf(id),
                "Transfer to user "+recipient.getUsername()+" | purpose="+(purpose==null?"":purpose));
        if (ok) {
            String incidentRef = incident.getIncidentId() == null ? String.valueOf(incident.getId()) : incident.getIncidentId();
            notificationService.createNotification(
                    recipient.getId(),
                    "Evidence custody transferred to you",
                    "Evidence #" + id + " from incident " + incidentRef
                            + " is now assigned to you as custodian."
                            + (purpose != null && !purpose.isBlank() ? " Purpose: " + purpose.trim() : ""),
                    "EVIDENCE_TRANSFER",
                    "/evidence/custody/" + id
            );
        }
        return "redirect:/evidence/custody/"+id;
    }

    @PostMapping("/custody/{id}/advance")
    public String advanceCustody(@PathVariable Long id,@RequestParam("action") String action,
                                 @RequestParam(value="purpose",required=false) String purpose,
                                 @RequestParam(value="notes",required=false) String notes,Authentication auth) {
        User actor=currentUser(auth); Evidence evidence=evidenceService.getById(id);
        if(evidence==null)return "redirect:/incidents";
        Incident incident=incidentRepository.findById(evidence.getIncidentId()).orElse(null);
        if(!canAccessEvidence(evidence,incident,actor))return "redirect:/access-denied";
        String normalized=action==null?"":action.trim().toUpperCase();
        if(!Set.of("ACCEPTED","UNDER_EXAMINATION","EXAMINED","REPORT_GENERATED").contains(normalized)){
            return "redirect:/access-denied";
        }
        boolean ok=evidenceService.advanceCustody(id,normalized,actor.getId(),auth.getName(),actor.getRole(),purpose,notes);
        auditService.log(ok?normalized+"_EVIDENCE":"FAILED_"+normalized+"_EVIDENCE","Evidence",String.valueOf(id),
                "Custody transition | resulting status="+evidence.getCustodyStatus());
        return "redirect:/evidence/custody/"+id;
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable Long id,Authentication auth)throws IOException{
        User user=currentUser(auth); Evidence evidence=evidenceService.getById(id);
        if(evidence==null)return ResponseEntity.notFound().build();
        Incident incident=incidentRepository.findById(evidence.getIncidentId()).orElse(null);
        if(!canAccessEvidence(evidence,incident,user))return ResponseEntity.status(403).build();
        byte[] data;
        try{data=evidenceService.downloadEvidence(id);}catch(IOException|RuntimeException e){return ResponseEntity.status(HttpStatus.NOT_FOUND).build();}
        auditService.log("DOWNLOAD_EVIDENCE","Evidence",String.valueOf(id),"Downloaded | SHA-256: "+evidence.getSha256Hash());
        String filename = evidence.getOriginalFilename() == null ? "evidence.bin" : evidence.getOriginalFilename()
                .replaceAll("[\\\\/\\r\
]", "_")
                .replace('\"', '_');
        String contentDisposition = "attachment; filename=" + '"' + filename + '"';
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/verify/{id}")
    public String verifyEvidence(@PathVariable Long id,Authentication auth){
        User user=currentUser(auth); Evidence evidence=evidenceService.getById(id);
        if(evidence==null)return "redirect:/incidents";
        Incident incident=incidentRepository.findById(evidence.getIncidentId()).orElse(null);
        if(!canAccessEvidence(evidence,incident,user))return "redirect:/access-denied";
        if (!canAcquireEvidence(user)) return "redirect:/access-denied";
        try {
            evidenceService.verifyEvidence(id,auth.getName(),user.getId(),user.getRole());
            auditService.log("VERIFY_EVIDENCE","Evidence",String.valueOf(id),"Verified | SHA-256: "+evidence.getSha256Hash());
        } catch (SecurityException | IllegalStateException e) {
            auditService.log("REJECT_VERIFY_EVIDENCE","Evidence",String.valueOf(id),e.getMessage());
        }
        return "redirect:/incidents/"+evidence.getIncidentId();
    }
}