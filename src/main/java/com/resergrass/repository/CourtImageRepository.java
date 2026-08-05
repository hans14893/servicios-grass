package com.resergrass.repository;

import com.resergrass.domain.entity.CourtImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourtImageRepository extends JpaRepository<CourtImage, Long> {
    List<CourtImage> findByCourtIdOrderBySortOrderAsc(Long courtId);
    void deleteByCourtId(Long courtId);
}
