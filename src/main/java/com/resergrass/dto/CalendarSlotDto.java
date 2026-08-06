package com.resergrass.dto;

import java.time.LocalTime;

public record CalendarSlotDto(
        Long courtId,
        String courtName,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        Long reservationId,
        String reservationName,
        String reservationPhone,
        LocalTime reservationStartTime,
        LocalTime reservationEndTime
) {
}
