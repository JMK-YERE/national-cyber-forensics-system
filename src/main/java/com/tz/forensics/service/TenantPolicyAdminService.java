package com.tz.forensics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tz.forensics.entity.Tenant;
import com.tz.forensics.entity.TenantPolicy;
import com.tz.forensics.repository.TenantPolicyRepository;
import com.tz.forensics.repository.TenantRepository;
import com.tz.forensics.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

@Service
public class TenantPolicyAdminService {
    private static final Set<String> POLICY_TYPES = Set.of(
        "EVIDENCE_RETENTION", "EXPORT_APPROVAL", "NOTIFICATIONS", "INCIDENT_SLA"
    );

    private final TenantRepository tenants;
    private final TenantPolicyRepository policies;
    private final TenantAuditEventService audit;
    private final UserRepository users;
    private final ObjectMapper mapper;

    public TenantPolicyAdminService(TenantRepository tenants, TenantPolicyRepository policies,
                                    TenantAuditEventService audit, UserRepository users,
                                    ObjectMapper mapper) {
        this.tenants = tenants;
        this.policies = policies;
        this.audit = audit;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public java.util.List<TenantPolicy> list(Long tenantId) {
        requireTenant(tenantId);
        return policies.findAll().stream().filter(p -> tenantId.equals(p.getTenantId()))
            .sorted(java.util.Comparator.comparing(TenantPolicy::getPolicyType)).toList();
    }

    @Transactional
    public TenantPolicy upsert(Long tenantId, String type, JsonNode document, String actorName) {
        Tenant tenant = requireTenant(tenantId);
        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Policies cannot be changed for an inactive tenant.");
        }
        String normalizedType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!POLICY_TYPES.contains(normalizedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported policy type.");
        }
        validateDocument(normalizedType, document);
        final String json;
        try {
            json = mapper.writeValueAsString(document);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy JSON could not be serialized.");
        }
        if (json.length() > 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy document exceeds 10000 characters.");
        }

        TenantPolicy policy = policies.findByTenantIdAndPolicyType(tenantId, normalizedType)
            .orElseGet(TenantPolicy::new);
        boolean created = policy.getId() == null;
        policy.setTenantId(tenantId);
        policy.setPolicyType(normalizedType);
        policy.setPolicyJson(json);
        policy.setVersion(created ? 1 : policy.getVersion() + 1);
        policy.setUpdatedBy(users.findByUsername(actorName).map(u -> u.getId()).orElse(null));
        TenantPolicy saved = policies.save(policy);
        audit.record(tenantId, saved.getUpdatedBy(), "TENANT_POLICY_" + (created ? "CREATED" : "UPDATED"),
            "tenant_policy", normalizedType, "SUCCESS", "Policy version " + saved.getVersion() + " saved.");
        return saved;
    }

    private Tenant requireTenant(Long tenantId) {
        return tenants.findById(tenantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found."));
    }

    private void validateDocument(String type, JsonNode document) {
        if (document == null || !document.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy must be a JSON object.");
        }
        if (containsSecretKey(document)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy documents must not contain secrets or credentials.");
        }
        switch (type) {
            case "EVIDENCE_RETENTION" -> {
                JsonNode days = document.get("retentionDays");
                if (days == null || !days.canConvertToInt() || days.intValue() < 1 || days.intValue() > 3650) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "retentionDays must be an integer from 1 to 3650.");
                }
                if (document.has("legalHoldEnabled") && !document.get("legalHoldEnabled").isBoolean()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "legalHoldEnabled must be boolean.");
                }
            }
            case "EXPORT_APPROVAL" -> {
                if (!document.has("requireApproval") || !document.get("requireApproval").isBoolean()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requireApproval must be boolean.");
                }
            }
            case "NOTIFICATIONS" -> {
                for (String field : Set.of("emailEnabled", "smsEnabled", "whatsappEnabled")) {
                    if (document.has(field) && !document.get(field).isBoolean()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be boolean.");
                    }
                }
            }
            case "INCIDENT_SLA" -> {
                for (String field : Set.of("responseMinutes", "containmentMinutes")) {
                    if (document.has(field) && (!document.get(field).canConvertToInt()
                            || document.get(field).intValue() < 1 || document.get(field).intValue() > 525600)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be between 1 and 525600 minutes.");
                    }
                }
                if (!document.has("responseMinutes") && !document.has("containmentMinutes")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one incident SLA target is required.");
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported policy type.");
        }
    }

    private boolean containsSecretKey(JsonNode node) {
        if (node.isObject()) {
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String key = names.next().toLowerCase(Locale.ROOT);
                if (key.contains("password") || key.contains("secret") || key.contains("token")
                        || key.contains("credential") || key.contains("apikey") || key.contains("api_key")) {
                    return true;
                }
                if (containsSecretKey(node.get(namesFor(node, key)))) return true;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) if (containsSecretKey(child)) return true;
        }
        return false;
    }

    private String namesFor(JsonNode node, String lowerName) {
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (name.toLowerCase(Locale.ROOT).equals(lowerName)) return name;
        }
        return lowerName;
    }
}
