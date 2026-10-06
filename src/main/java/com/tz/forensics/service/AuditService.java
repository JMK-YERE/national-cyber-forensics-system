package com.tz.forensics.service;

import com.tz.forensics.entity.AuditLog;
import com.tz.forensics.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String action, String entityType, String entityId, String details) {
        log(null, null, action, entityType, entityId, details);
    }

    /** Records an auditable action with an explicit actor when the caller already resolved it. */
    public void log(Long actorId, String actorName, String action, String entityType, String entityId, String details) {
        AuditLog log = new AuditLog();
        log.setUserId(actorId);
        log.setUsername(actorName);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setIpAddress(getClientIp());

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                if (log.getUsername() == null || log.getUsername().isBlank()) log.setUsername(auth.getName());
            }
        } catch (Exception ignored) {}

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) log.setUserAgent(attrs.getRequest().getHeader("User-Agent"));
        } catch (Exception ignored) {}

        auditLogRepository.save(log);
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                // With server.forward-headers-strategy=framework, Spring resolves trusted
                // proxy forwarding headers. Do not manually trust the first X-Forwarded-For
                // value because clients can spoof that header before the trusted proxy layer.
                String ip = attrs.getRequest().getRemoteAddr();
                if (ip != null && !ip.isBlank()) return ip;
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}
