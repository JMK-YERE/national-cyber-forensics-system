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

    // ===== CREATE NOTIFICATION =====
    public void createNotification(String title, String message, String type, String linkUrl) {
        Notification notification = new Notification(null, title, message, type, linkUrl);
        notificationRepository.save(notification);
    }

    // ===== INCIDENT NOTIFICATION =====
    public void createIncidentNotification(String incidentId, String title, String severity) {
        String emoji;
        if ("CRITICAL".equalsIgnoreCase(severity)) emoji = "🚨";
        else if ("HIGH".equalsIgnoreCase(severity)) emoji = "⚠️";
        else if ("MEDIUM".equalsIgnoreCase(severity)) emoji = "📌";
        else emoji = "ℹ️";

        String notifTitle = emoji + " Tukio Jipya: " + incidentId;
        String notifMessage = title + " (Severity: " + severity + ")";
        String notifType = (severity != null) ? severity.toUpperCase() : "INFO";

        createNotification(notifTitle, notifMessage, notifType, "/report-attack");
    }

    // ===== GET ALL =====
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    // ===== GET UNREAD =====
    public List<Notification> getUnreadNotifications() {
        return notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
    }

    // ===== GET LATEST 10 =====
    public List<Notification> getLatestNotifications() {
        return notificationRepository.findTop10ByOrderByCreatedAtDesc();
    }

    // ===== COUNT UNREAD =====
    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }

    // ===== MARK AS READ =====
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    // ===== MARK ALL AS READ =====
    public void markAllAsRead() {
        List<Notification> unread = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }
}
