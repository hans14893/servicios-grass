package com.resergrass.repository;

import com.resergrass.domain.entity.CourtPriceRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourtPriceRuleRepository extends JpaRepository<CourtPriceRule, Long> {
    List<CourtPriceRule> findByCourtIdAndActiveTrueOrderByStartTimeAsc(Long courtId);
    List<CourtPriceRule> findByCourtIdOrderByStartTimeAsc(Long courtId);
    void deleteByCourtId(Long courtId);
}
