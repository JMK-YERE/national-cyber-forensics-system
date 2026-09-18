package com.tz.forensics.service;

import com.tz.forensics.entity.Notification;
import com.tz.forensics.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void createNotification(String title, String message, String type, String linkUrl) {
        Notification notification = new Notification(null, title, message, type, linkUrl);
        notificationRepository.save(notification);
    }

    public void createIncidentNotification(String incidentId, String title, String severity) {
        String emoji = switch (severity != null ? severity.toUpperCase() : "MEDIUM") {
            case "CRITICAL" -> "🚨";
            case "HIGH" -> "⚠️";
            case "MEDIUM" -> "📌";
            default -> "ℹ️";
        };
        createNotification(
                emoji + " Tukio Jipya: " + incidentId,
                title + " (Severity: " + severity + ")",
                severity != null ? severity.toUpperCase() : "INFO",
                "/incidents"
        );
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Notification> getUnreadNotifications() {
        return notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
    }

    public List<Notification> getLatestNotifications() {
        return notificationRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }

    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead() {
        List<Notification> unread = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }
}
