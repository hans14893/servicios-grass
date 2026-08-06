package com.resergrass.dto;

public record CourtOperationDto(
        Long courtId,
        String courtName,
        String status,
        OperationsReservationDto currentReservation,
        OperationsReservationDto nextReservation
) {
}
