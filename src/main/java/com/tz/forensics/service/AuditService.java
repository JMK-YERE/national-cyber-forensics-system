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
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setIpAddress(getClientIp());

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                log.setUsername(auth.getName());
            }
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
                HttpServletRequest request = attrs.getRequest();
                String xff = request.getHeader("X-Forwarded-For");
                if (xff != null && !xff.isEmpty()) return xff.split(",")[0];
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}
