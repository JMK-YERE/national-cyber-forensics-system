package com.tz.forensics.service;

import com.tz.forensics.entity.Notification;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.NotificationRepository;
import com.tz.forensics.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public NotificationService(NotificationRepository notificationRepository,
                                UserRepository userRepository,
                                AuditService auditService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Creates a notification for exactly one user.
     * Notification ownership is always tied to the target userId; callers must
     * never use this method as an unscoped broadcast primitive.
     */
    public void createNotification(Long userId, String title, String message, String type, String linkUrl) {
        if (userId == null) {
            log.warn("Notification skipped: userId is null | title={}", title);
            return;
        }

        String safeTitle = normalizeRequired(title, 200, "Notification title");
        String safeMessage = normalizeOptional(message, 5000);
        String safeType = normalizeType(type);
        String safeLink = normalizeLink(linkUrl);

        Notification n = new Notification(userId, safeTitle, safeMessage, safeType, safeLink);
        Notification saved = notificationRepository.save(n);

        auditService.log("CREATE_NOTIFICATION", "Notification", String.valueOf(saved.getId()),
                "Target userId=" + userId + " | type=" + safeType);
    }

    /**
     * Backward-compatible broadcast for legacy callers.
     * It is intentionally limited to operational staff and does not include
     * INDIVIDUAL users.
     */
    public void createNotification(String title, String message, String type, String linkUrl) {
        try {
            List<User> staff = userRepository.findAll().stream()
                    .filter(u -> u.getId() != null
                            && Boolean.TRUE.equals(u.getEnabled())
                            && u.isApproved()
                            && (u.isAdmin() || u.isProfessional() || u.isForensics()
                                || "ANALYST".equalsIgnoreCase(u.getRole())))
                    .toList();
            for (User user : staff) {
                createNotification(user.getId(), title, message, type, linkUrl);
            }
        } catch (Exception e) {
            log.error("createNotification (broadcast) failed: {}", e.getMessage());
            auditService.log("FAILED_CREATE_NOTIFICATION", "Notification", "BROADCAST",
                    e.getMessage() == null ? "Broadcast notification failed" : e.getMessage());
        }
    }

    public void createIncidentNotification(String incidentId, String title, String severity) {
        try {
            String normalizedSeverity = severity == null ? "INFO" : severity.trim().toUpperCase();
            String notifTitle = "Incident: " + (incidentId == null ? "unknown" : incidentId);
            String notifMessage = (title == null ? "New incident reported." : title.trim())
                    + " (Severity: " + normalizedSeverity + ")";

            List<User> staff = userRepository.findAll().stream()
                    .filter(u -> u.getId() != null
                            && Boolean.TRUE.equals(u.getEnabled())
                            && u.isApproved()
                            && (u.isAdmin() || u.isProfessional() || u.isForensics()))
                    .toList();
            for (User user : staff) {
                createNotification(user.getId(), notifTitle, notifMessage,
                        normalizedSeverity, "/incidents");
            }
        } catch (Exception e) {
            log.error("createIncidentNotification failed: {}", e.getMessage());
            auditService.log("FAILED_CREATE_NOTIFICATION", "Notification",
                    incidentId == null ? "unknown" : incidentId,
                    e.getMessage() == null ? "Incident notification failed" : e.getMessage());
        }
    }

    public void createReportNotification(String reportId, String title, String severity,
                                         Long userId, String linkUrl) {
        String normalizedSeverity = severity == null ? "INFO" : severity.trim().toUpperCase();
        createNotification(userId,
                "Report " + (reportId == null ? "unknown" : reportId),
                title,
                normalizedSeverity,
                linkUrl);
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        return userId == null ? List.of()
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return userId == null ? List.of()
                : notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return userId == null ? 0
                : notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public boolean markAsReadForUser(Long id, Long userId) {
        if (id == null || userId == null) return false;
        return notificationRepository.findById(id)
                .filter(n -> userId.equals(n.getUserId()))
                .map(n -> {
                    if (!Boolean.TRUE.equals(n.getIsRead())) {
                        n.setIsRead(true);
                        notificationRepository.save(n);
                        auditService.log("READ_NOTIFICATION", "Notification", String.valueOf(id),
                                "Notification marked read");
                    }
                    return true;
                })
                .orElse(false);
    }

    /**
     * Legacy method retained for internal compatibility, but intentionally
     * does nothing without an owner. This prevents IDOR through an unscoped
     * notification id.
     */
    public boolean markAsRead(Long id) {
        log.warn("Unscoped markAsRead rejected for notificationId={}", id);
        return false;
    }

    public int markAllAsRead(Long userId) {
        if (userId == null) return 0;
        List<Notification> unread =
                notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        if (!unread.isEmpty()) {
            notificationRepository.saveAll(unread);
            auditService.log("READ_ALL_NOTIFICATIONS", "Notification", String.valueOf(userId),
                    "Marked " + unread.size() + " notification(s) read");
        }
        return unread.size();
    }

    public boolean deleteForUser(Long id, Long userId) {
        if (id == null || userId == null) return false;
        return notificationRepository.findById(id)
                .filter(n -> userId.equals(n.getUserId()))
                .map(n -> {
                    notificationRepository.delete(n);
                    auditService.log("DELETE_NOTIFICATION", "Notification", String.valueOf(id),
                            "Notification deleted by owner");
                    return true;
                })
                .orElse(false);
    }

    private String normalizeRequired(String value, int maxLength, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required.");
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters.");
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (value == null) return "";
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("Notification message exceeds " + maxLength + " characters.");
        }
        return normalized;
    }

    private String normalizeType(String type) {
        String normalized = type == null || type.isBlank() ? "INFO" : type.trim().toUpperCase();
        return normalized.length() > 30 ? normalized.substring(0, 30) : normalized;
    }

    private String normalizeLink(String linkUrl) {
        if (linkUrl == null || linkUrl.isBlank()) return null;
        String link = linkUrl.trim();
        if (link.length() > 500) throw new IllegalArgumentException("Notification link is too long.");
        if (!link.startsWith("/") || link.startsWith("//")) {
            throw new IllegalArgumentException("Notification links must be local application paths.");
        }
        return link;
    }
}
