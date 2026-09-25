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

    public NotificationService(NotificationRepository notificationRepository,
                                UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ===== MAIN METHOD — with userId =====
    public void createNotification(Long userId, String title, String message, String type, String linkUrl) {
        if (userId == null) {
            log.warn("Notification skipped: userId is null | title={}", title);
            return;
        }
        Notification n = new Notification(userId, title, message, type, linkUrl);
        notificationRepository.save(n);
    }

    // ===== BACKWARD COMPAT — without userId — sends to all admins =====
    public void createNotification(String title, String message, String type, String linkUrl) {
        try {
            List<User> admins = userRepository.findAll().stream()
                    .filter(u -> u.isAdmin() || u.isProfessional() || u.isForensics())
                    .toList();
            for (User admin : admins) {
                createNotification(admin.getId(), title, message, type, linkUrl);
            }
        } catch (Exception e) {
            log.error("createNotification (broadcast) failed: {}", e.getMessage());
        }
    }

    // ===== BACKWARD COMPAT — createIncidentNotification =====
    public void createIncidentNotification(String incidentId, String title, String severity) {
        try {
            String emoji;
            if ("CRITICAL".equalsIgnoreCase(severity)) emoji = "🚨";
            else if ("HIGH".equalsIgnoreCase(severity)) emoji = "⚠️";
            else if ("MEDIUM".equalsIgnoreCase(severity)) emoji = "📌";
            else emoji = "ℹ️";

            String notifTitle = emoji + " Tukio: " + incidentId;
            String notifMessage = title + " (Severity: " + severity + ")";
            String notifType = (severity != null) ? severity.toUpperCase() : "INFO";

            // Send to all admins
            List<User> admins = userRepository.findAll().stream()
                    .filter(u -> u.isAdmin() || u.isProfessional() || u.isForensics())
                    .toList();
            for (User admin : admins) {
                createNotification(admin.getId(), notifTitle, notifMessage, notifType, "/report-attack");
            }
        } catch (Exception e) {
            log.error("createIncidentNotification failed: {}", e.getMessage());
        }
    }

    // ===== CREATE REPORT NOTIFICATION (convenience) =====
    public void createReportNotification(String reportId, String title, String severity, Long userId, String linkUrl) {
        String emoji;
        if ("CRITICAL".equalsIgnoreCase(severity)) emoji = "🚨";
        else if ("HIGH".equalsIgnoreCase(severity)) emoji = "⚠️";
        else emoji = "📌";

        createNotification(userId, emoji + " Report " + reportId, title,
                severity != null ? severity.toUpperCase() : "INFO", linkUrl);
    }

    // ===== GETTERS =====
    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // ===== MARK AS READ =====
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }
}
