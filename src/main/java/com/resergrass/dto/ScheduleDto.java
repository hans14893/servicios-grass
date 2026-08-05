package com.resergrass.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleDto(Long id, Long courtId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, boolean active) {
}
