package com.tz.forensics.service;

import com.tz.forensics.entity.Announcement;
import com.tz.forensics.entity.SlideshowImage;
import com.tz.forensics.repository.AnnouncementRepository;
import com.tz.forensics.repository.SlideshowImageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LandingService {

    private final AnnouncementRepository announcementRepo;
    private final SlideshowImageRepository slideshowRepo;

    public LandingService(AnnouncementRepository announcementRepo,
                          SlideshowImageRepository slideshowRepo) {
        this.announcementRepo = announcementRepo;
        this.slideshowRepo = slideshowRepo;
    }

    public List<Announcement> getActiveAnnouncements() {
        return announcementRepo.findByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
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
        return slideshowRepo.findByActiveTrueOrderByDisplayOrderAsc();
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
