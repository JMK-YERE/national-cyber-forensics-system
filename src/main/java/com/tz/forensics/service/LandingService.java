package com.tz.forensics.service;

import com.tz.forensics.entity.Announcement;
import com.tz.forensics.entity.SlideshowImage;
import com.tz.forensics.repository.AnnouncementRepository;
import com.tz.forensics.repository.SlideshowImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LandingService {

    private static final Logger log = LoggerFactory.getLogger(LandingService.class);

    private final AnnouncementRepository announcementRepo;
    private final SlideshowImageRepository slideshowRepo;

    public LandingService(AnnouncementRepository announcementRepo,
                          SlideshowImageRepository slideshowRepo) {
        this.announcementRepo = announcementRepo;
        this.slideshowRepo = slideshowRepo;
    }

    public List<Announcement> getActiveAnnouncements() {
        List<Announcement> list = announcementRepo.findByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
        log.info("Announcements: {}", list.size());
        return list;
    }

    public List<Announcement> getAllAnnouncements() {
        return announcementRepo.findAllByOrderByCreatedAtDesc();
    }

    public Announcement saveAnnouncement(Announcement a) {
        return announcementRepo.save(a);
    }

    public Announcement getAnnouncementById(Long id) {
        return announcementRepo.findById(id).orElse(null);
    }

    public void deleteAnnouncement(Long id) {
        announcementRepo.deleteById(id);
    }

    public List<SlideshowImage> getActiveSlides() {
        List<SlideshowImage> list = slideshowRepo.findByActiveTrueOrderByDisplayOrderAsc();
        log.info("Slides: {}", list.size());
        return list;
    }

    public List<SlideshowImage> getAllSlides() {
        return slideshowRepo.findAllByOrderByDisplayOrderAsc();
    }

    public SlideshowImage saveSlide(SlideshowImage s) {
        return slideshowRepo.save(s);
    }

    public SlideshowImage getSlideById(Long id) {
        return slideshowRepo.findById(id).orElse(null);
    }

    public void deleteSlide(Long id) {
        slideshowRepo.deleteById(id);
    }
}
