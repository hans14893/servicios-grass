package com.resergrass.repository;

import com.resergrass.domain.entity.AvailableSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface AvailableScheduleRepository extends JpaRepository<AvailableSchedule, Long> {
    List<AvailableSchedule> findByCourtIdAndDayOfWeekAndActiveTrue(Long courtId, DayOfWeek dayOfWeek);
    List<AvailableSchedule> findByCourtIdOrderByDayOfWeekAscStartTimeAsc(Long courtId);
    void deleteByCourtId(Long courtId);
}
