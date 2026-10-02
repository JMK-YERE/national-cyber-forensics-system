package com.tz.forensics.repository;

import com.tz.forensics.entity.SlideshowImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SlideshowImageRepository extends JpaRepository<SlideshowImage, Long> {
    List<SlideshowImage> findByActiveTrueOrderByDisplayOrderAsc();
    List<SlideshowImage> findAllByOrderByDisplayOrderAsc();
}
