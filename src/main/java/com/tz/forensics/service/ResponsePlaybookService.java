package com.tz.forensics.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Curated, versioned starter playbooks for repeatable incident response.
 * These are operational guidance templates, not automatic containment actions.
 */
@Service
public class ResponsePlaybookService {
    public record Step(int order, String phase, String title, String guidance, boolean required) {}
    public record Playbook(String slug, String title, String summary, String severityHint,
                           String version, List<String> triggers, List<Step> steps) {}

    private final Map<String, Playbook> playbooks = Map.of(
        "phishing", new Playbook(
            "phishing", "Phishing and Business Email Compromise",
            "Triage suspicious messages, protect affected identities and preserve email evidence.",
            "HIGH", "1.0",
            List.of("Suspicious email", "Credential harvesting", "Payment redirection", "Mailbox compromise"),
            List.of(
                step(1, "DETECT", "Register and classify the report", "Record reporter, received time with timezone, affected mailbox, message ID and business impact. Do not forward the original message in a way that loses headers.", true),
                step(2, "PRESERVE", "Preserve the original message", "Export the original email with full headers and attachments to the approved evidence vault. Record collector, method, source, time and SHA-256; restrict access to the original.", true),
                step(3, "TRIAGE", "Validate indicators and scope", "Review sender, reply-to, URLs, attachment hashes, authentication results and related reports. Use approved analysis tools; do not open suspicious files on a production workstation.", true),
                step(4, "CONTAIN", "Protect affected accounts", "With the account owner and authorized administrator, revoke sessions, reset credentials, review MFA and block confirmed indicators. Record approver, action and timestamp.", true),
                step(5, "ERADICATE", "Remove persistence and malicious rules", "Review inbox rules, forwarding, OAuth grants and sent-mail activity; remove confirmed malicious artifacts and preserve relevant logs first.", true),
                step(6, "RECOVER", "Restore and monitor", "Restore trusted access, verify MFA and monitor for repeat authentication or payment activity.", true),
                step(7, "LEARN", "Document findings and notify", "Record impact, root cause, evidence references, decisions, notifications and corrective actions in the case report.", true)
            )),
        "ransomware", new Playbook(
            "ransomware", "Ransomware or Destructive Malware",
            "Coordinate safe containment, volatile evidence capture, recovery and business continuity.",
            "CRITICAL", "1.0",
            List.of("Mass file encryption", "Ransom note", "Destructive malware", "Backup compromise"),
            List.of(
                step(1, "DETECT", "Declare incident and establish command", "Assign incident lead, severity, communications channel and decision log. Identify safety-critical and business-critical systems.", true),
                step(2, "PRESERVE", "Capture time-sensitive evidence", "Where safe and authorized, capture volatile data and security telemetry before shutdown. Record system clock/timezone, collector, tools and hashes.", true),
                step(3, "CONTAIN", "Isolate affected systems", "Use approved network controls to isolate affected hosts and stop spread. Avoid powering off systems unless required for safety or containment and document the rationale.", true),
                step(4, "CONTAIN", "Protect identity and backup planes", "Review privileged sessions, service accounts, remote access and backup administration. Restrict compromised paths without destroying logs.", true),
                step(5, "ERADICATE", "Determine scope and persistence", "Hunt for initial access, lateral movement, scheduled tasks, malicious accounts and affected data. Preserve representative samples safely.", true),
                step(6, "RECOVER", "Restore from verified clean sources", "Validate backups, rebuild trusted systems, rotate credentials and restore in priority order with business-owner approval.", true),
                step(7, "LEARN", "Validate recovery and report", "Monitor for reinfection, record affected assets and data, document decisions and assign remediation owners and due dates.", true)
            )),
        "account-compromise", new Playbook(
            "account-compromise", "Account or Privileged Identity Compromise",
            "Investigate suspicious sign-ins, contain identity abuse and recover trusted access.",
            "HIGH", "1.0",
            List.of("Impossible travel", "MFA fatigue", "Stolen session", "Unauthorized administrator"),
            List.of(
                step(1, "TRIAGE", "Validate identity alert", "Capture account ID, alert source, event IDs, time window, source IPs and business context.", true),
                step(2, "PRESERVE", "Export identity and endpoint logs", "Preserve authentication, MFA, VPN, endpoint and audit logs with collection metadata and hashes where applicable.", true),
                step(3, "CONTAIN", "Revoke sessions and credentials", "With the identity owner, revoke active sessions/tokens, reset credentials and rotate exposed secrets. Preserve relevant sign-in events first where feasible.", true),
                step(4, "INVESTIGATE", "Check persistence and lateral movement", "Review MFA methods, recovery options, OAuth grants, mailbox rules, privileged role changes and activity performed by the account.", true),
                step(5, "RECOVER", "Restore trusted identity", "Re-enrol strong MFA, remove unauthorized methods and grants, and confirm access with the account owner.", true),
                step(6, "LEARN", "Record impact and controls", "Document accessed resources, possible data exposure, notifications and preventive changes.", true)
            )),
        "data-breach", new Playbook(
            "data-breach", "Suspected Data Breach or Exfiltration",
            "Establish what data may be affected while preserving evidence and controlling disclosure.",
            "CRITICAL", "1.0",
            List.of("Unusual outbound transfer", "Public data exposure", "Unauthorized access", "Lost device"),
            List.of(
                step(1, "TRIAGE", "Open restricted incident", "Limit case access to need-to-know responders. Record systems, data categories, discovery time and known recipients.", true),
                step(2, "PRESERVE", "Preserve access and transfer evidence", "Secure cloud audit, proxy, DLP, database, endpoint and identity logs; retain original exports and collection metadata.", true),
                step(3, "CONTAIN", "Stop ongoing unauthorized access", "Revoke exposed links or credentials and restrict confirmed exfiltration paths with an authorized approver. Preserve evidence of the change.", true),
                step(4, "ASSESS", "Determine data and people in scope", "Identify data types, volume, affected systems, time window and confidence. Separate confirmed facts from hypotheses.", true),
                step(5, "ESCALATE", "Route legal and notification decisions", "Refer notification and regulatory obligations to the organization's privacy/legal lead. Do not make unsupported legal conclusions in the technical record.", true),
                step(6, "RECOVER", "Remediate exposure", "Remove unauthorized access, patch the cause, rotate secrets and verify that exposure has ended.", true),
                step(7, "LEARN", "Finalize controlled report", "Record findings, uncertainty, evidence, approvals, notifications and corrective actions under the approved retention policy.", true)
            )),
        "malware", new Playbook(
            "malware", "Malware or Suspicious Endpoint Activity",
            "Safely triage a suspected infected endpoint and preserve forensic artifacts.",
            "HIGH", "1.0",
            List.of("EDR malware alert", "Suspicious process", "Persistence alert", "Unexpected network beacon"),
            List.of(
                step(1, "TRIAGE", "Identify affected host and user", "Record hostname, asset owner, logged-in users, alert IDs, detection time and business criticality.", true),
                step(2, "PRESERVE", "Collect authorized artifacts", "Capture relevant EDR telemetry, process/network details and volatile evidence when trained personnel can do so safely. Record tools and times.", true),
                step(3, "CONTAIN", "Isolate endpoint", "Use approved EDR/network isolation; document exceptions for critical systems and who authorized the action.", true),
                step(4, "EXAMINE", "Analyze samples in a safe environment", "Calculate SHA-256, inspect metadata and analyze in an isolated sandbox. Never execute unknown samples on production systems.", true),
                step(5, "ERADICATE", "Remove confirmed malicious artifacts", "Identify persistence and initial access, remediate the root cause and preserve required artifacts before removal.", true),
                step(6, "RECOVER", "Rebuild or restore and monitor", "Restore from a trusted baseline, patch, rotate exposed credentials and monitor for recurring indicators.", true),
                step(7, "LEARN", "Close with evidence-backed findings", "Link evidence and IOC records, note confidence and limitations, and assign follow-up actions.", true)
            ))
    );

    public List<Playbook> list() {
        return playbooks.values().stream()
            .sorted(java.util.Comparator.comparing(Playbook::title))
            .toList();
    }

    public Optional<Playbook> findBySlug(String slug) {
        return Optional.ofNullable(playbooks.get(slug == null ? "" : slug.trim().toLowerCase()));
    }

    private static Step step(int order, String phase, String title, String guidance, boolean required) {
        return new Step(order, phase, title, guidance, required);
    }
}
