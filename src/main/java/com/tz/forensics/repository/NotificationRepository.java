package com.tz.forensics.repository;

import com.tz.forensics.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByOrderByCreatedAtDesc();
    List<Notification> findByIsReadFalseOrderByCreatedAtDesc();
    long countByIsReadFalse();
    List<Notification> findTop10ByOrderByCreatedAtDesc();
}
