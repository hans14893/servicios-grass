package com.resergrass.repository;

import com.resergrass.domain.entity.CourtPromotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourtPromotionRepository extends JpaRepository<CourtPromotion, Long> {
    List<CourtPromotion> findByCourtIdAndActiveTrueOrderByNameAsc(Long courtId);
    List<CourtPromotion> findByCourtIdOrderByNameAsc(Long courtId);
    void deleteByCourtId(Long courtId);
}
