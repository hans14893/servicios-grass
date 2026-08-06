package com.resergrass.dto;

import java.time.LocalDate;
import java.util.List;

public record ClientDashboardDto(
        LocalDate date,
        List<ClientCourtAvailabilityDto> nextAvailableSlots,
        ClientReservationSummaryDto nextReservation
) {
}
