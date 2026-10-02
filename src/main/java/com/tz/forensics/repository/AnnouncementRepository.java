package com.tz.forensics.repository;

import com.tz.forensics.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
    List<Announcement> findAllByOrderByCreatedAtDesc();
}
