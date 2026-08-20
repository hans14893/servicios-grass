package com.resergrass.service;

import com.resergrass.domain.entity.*;
import com.resergrass.domain.enums.CourtStatus;
import com.resergrass.domain.enums.PriceDayType;
import com.resergrass.domain.enums.ReservationStatus;
import com.resergrass.dto.*;
import com.resergrass.exception.ApiException;
import com.resergrass.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourtService {
    private final CourtRepository courtRepository;
    private final CourtImageRepository imageRepository;
    private final CourtPriceRuleRepository priceRuleRepository;
    private final CourtPromotionRepository promotionRepository;
    private final AvailableScheduleRepository scheduleRepository;
    private final ReservationRepository reservationRepository;
    private final CourtPricingService pricingService;

    public List<CourtDto> activeCourts() {
        var courts = courtRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(court -> court.getStatus() != CourtStatus.DESHABILITADA)
                .map(this::toDto).toList();
        log.info("COURT_LIST_ACTIVE count={}", courts.size());
        return courts;
    }

    public List<CourtDto> all() {
        var courts = courtRepository.findAll().stream().map(this::toDto).toList();
        log.info("COURT_LIST_ALL count={}", courts.size());
        return courts;
    }

    public CourtDto byId(Long id) {
        log.info("COURT_GET id={}", id);
        return toDto(findCourt(id));
    }

    @Transactional
    public CourtDto create(CourtRequest request) {
        log.info("COURT_CREATE_REQUEST name={} code={} type={} status={}", request.name(), request.code(), request.type(), request.status());
        var court = new Court();
        apply(court, request);
        var saved = courtRepository.save(court);
        replaceChildren(saved, request);
        log.info("COURT_CREATE_SUCCESS id={} name={} status={}", saved.getId(), saved.getName(), saved.getStatus());
        return toDto(saved);
    }

    @Transactional
    public CourtDto update(Long id, CourtRequest request) {
        log.info("COURT_UPDATE_REQUEST id={} name={} status={}", id, request.name(), request.status());
        var court = findCourt(id);
        apply(court, request);
        var saved = courtRepository.save(court);
        replaceChildren(saved, request);
        log.info("COURT_UPDATE_SUCCESS id={} name={} status={}", saved.getId(), saved.getName(), saved.getStatus());
        return toDto(saved);
    }

    @Transactional
    public CourtDto changeStatus(Long id, CourtStatus status) {
        log.info("COURT_STATUS_REQUEST id={} status={}", id, status);
        var court = findCourt(id);
        court.setStatus(status);
        court.setActive(status != CourtStatus.DESHABILITADA);
        var saved = courtRepository.save(court);
        log.info("COURT_STATUS_SUCCESS id={} active={} status={}", saved.getId(), saved.isActive(), saved.getStatus());
        return toDto(saved);
    }

    @Transactional
    public void deactivate(Long id) {
        log.warn("COURT_DEACTIVATE_REQUEST id={}", id);
        changeStatus(id, CourtStatus.DESHABILITADA);
    }

    public CourtStatsDto stats(Long id) {
        log.info("COURT_STATS_REQUEST id={}", id);
        var court = findCourt(id);
        var reservations = reservationRepository.findByCourtIdOrderByReservationDateDescStartTimeAsc(id);
        var income = reservations.stream()
                .filter(reservation -> reservation.getStatus() != ReservationStatus.CANCELADA)
                .map(Reservation::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var stats = new CourtStatsDto(
                court.getId(),
                court.getName(),
                reservations.size(),
                reservations.stream().filter(r -> r.getStatus() == ReservationStatus.CONFIRMADA).count(),
                reservations.stream().filter(r -> r.getStatus() == ReservationStatus.CANCELADA).count(),
                income
        );
        log.info("COURT_STATS_SUCCESS id={} total={} confirmed={} cancelled={} income={}", id, stats.totalReservations(), stats.confirmedReservations(), stats.cancelledReservations(), stats.projectedIncome());
        return stats;
    }

    @Transactional
    public CourtPriceRuleDto addPriceRule(Long courtId, CourtPriceRuleRequest request) {
        log.info("COURT_PRICE_RULE_CREATE_REQUEST courtId={} dayType={} start={} end={} hourlyPrice={}", courtId, request.dayType(), request.startTime(), request.endTime(), request.hourlyPrice());
        var court = findCourt(courtId);
        validateTimeRange(request.startTime(), request.endTime());
        var rule = new CourtPriceRule();
        apply(rule, court, request);
        var saved = priceRuleRepository.save(rule);
        log.info("COURT_PRICE_RULE_CREATE_SUCCESS id={} courtId={}", saved.getId(), courtId);
        return toDto(saved);
    }

    @Transactional
    public CourtPromotionDto addPromotion(Long courtId, CourtPromotionRequest request) {
        log.info("COURT_PROMOTION_CREATE_REQUEST courtId={} name={} type={}", courtId, request.name(), request.type());
        var court = findCourt(courtId);
        var promotion = new CourtPromotion();
        apply(promotion, court, request);
        var saved = promotionRepository.save(promotion);
        log.info("COURT_PROMOTION_CREATE_SUCCESS id={} courtId={} name={}", saved.getId(), courtId, saved.getName());
        return toDto(saved);
    }

    private Court findCourt(Long id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cancha no encontrada"));
    }

    private void apply(Court court, CourtRequest request) {
        court.setName(request.name());
        court.setCode(request.code());
        court.setDescription(request.description());
        court.setMainImageUrl(request.mainImageUrl());
        if (request.type() != null) {
            court.setType(request.type());
        }
        court.setDimensions(request.dimensions());
        court.setMaxPlayers(request.maxPlayers() == null ? 0 : request.maxPlayers());
        court.setStatus(request.status() == null ? CourtStatus.DISPONIBLE : request.status());
        court.setActive(request.active() && court.getStatus() != CourtStatus.DESHABILITADA);
    }

    private void replaceChildren(Court court, CourtRequest request) {
        imageRepository.deleteByCourtId(court.getId());
        priceRuleRepository.deleteByCourtId(court.getId());
        promotionRepository.deleteByCourtId(court.getId());
        scheduleRepository.deleteByCourtId(court.getId());

        var images = new ArrayList<String>();
        if (request.mainImageUrl() != null && !request.mainImageUrl().isBlank()) {
            images.add(request.mainImageUrl());
        }
        if (request.gallery() != null) {
            images.addAll(request.gallery());
        }
        for (int i = 0; i < images.size(); i++) {
            var image = new CourtImage();
            image.setCourt(court);
            image.setImageUrl(images.get(i));
            image.setSortOrder(i);
            imageRepository.save(image);
        }

        if (request.priceRules() != null) {
            request.priceRules().forEach(price -> {
                validateTimeRange(price.startTime(), price.endTime());
                var rule = new CourtPriceRule();
                apply(rule, court, price);
                priceRuleRepository.save(rule);
            });
        }

        if (request.schedules() != null) {
            request.schedules().forEach(scheduleRequest -> {
                validateTimeRange(scheduleRequest.startTime(), scheduleRequest.endTime());
                var schedule = new AvailableSchedule();
                schedule.setCourt(court);
                schedule.setDayOfWeek(scheduleRequest.dayOfWeek());
                schedule.setStartTime(scheduleRequest.startTime());
                schedule.setEndTime(scheduleRequest.endTime());
                schedule.setActive(scheduleRequest.active());
                scheduleRepository.save(schedule);
            });
        }

        if (request.promotions() != null) {
            request.promotions().forEach(promotionRequest -> {
                var promotion = new CourtPromotion();
                apply(promotion, court, promotionRequest);
                promotionRepository.save(promotion);
            });
        }
        log.info("COURT_CHILDREN_REPLACED courtId={} images={} priceRules={} schedules={} promotions={}",
                court.getId(),
                images.size(),
                request.priceRules() == null ? 0 : request.priceRules().size(),
                request.schedules() == null ? 0 : request.schedules().size(),
                request.promotions() == null ? 0 : request.promotions().size());
    }

    private void apply(CourtPriceRule rule, Court court, CourtPriceRuleRequest request) {
        rule.setCourt(court);
        rule.setDayType(request.dayType());
        rule.setDayOfWeek(request.dayType() == PriceDayType.SPECIFIC_DAY ? request.dayOfWeek() : null);
        rule.setStartTime(request.startTime());
        rule.setEndTime(request.endTime());
        rule.setHourlyPrice(request.hourlyPrice());
        rule.setHalfHourPrice(request.halfHourPrice());
        rule.setActive(request.active());
    }

    private void apply(CourtPromotion promotion, Court court, CourtPromotionRequest request) {
        promotion.setCourt(court);
        promotion.setName(request.name());
        promotion.setType(request.type());
        promotion.setFixedPrice(request.fixedPrice());
        promotion.setDiscountPercent(request.discountPercent());
        promotion.setRequiredHours(request.requiredHours());
        promotion.setStartTime(request.startTime());
        promotion.setEndTime(request.endTime());
        promotion.setValidFrom(request.validFrom());
        promotion.setValidTo(request.validTo());
        promotion.setActive(request.active());
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || startTime.equals(endTime)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La hora de inicio y fin no pueden ser iguales");
        }
    }

    private CourtDto toDto(Court court) {
        var priceRules = priceRuleRepository.findByCourtIdOrderByStartTimeAsc(court.getId()).stream().map(this::toDto).toList();
        var schedules = scheduleRepository.findByCourtIdOrderByDayOfWeekAscStartTimeAsc(court.getId()).stream().map(this::toDto).toList();
        var promotions = promotionRepository.findByCourtIdOrderByNameAsc(court.getId()).stream().map(this::toDto).toList();
        var images = imageRepository.findByCourtIdOrderBySortOrderAsc(court.getId()).stream().map(CourtImage::getImageUrl).toList();
        return new CourtDto(
                court.getId(),
                court.getName(),
                court.getCode(),
                court.getDescription(),
                court.getMainImageUrl(),
                images,
                court.getType(),
                court.getDimensions(),
                court.getMaxPlayers(),
                court.getStatus(),
                pricingService.displayHourlyPrice(court.getId()),
                pricingService.displayHalfHourPrice(court.getId()),
                court.isActive(),
                priceRules,
                schedules,
                promotions
        );
    }

    private CourtPriceRuleDto toDto(CourtPriceRule rule) {
        return new CourtPriceRuleDto(rule.getId(), rule.getCourt().getId(), rule.getDayType(), rule.getDayOfWeek(), rule.getStartTime(), rule.getEndTime(), rule.getHourlyPrice(), rule.getHalfHourPrice(), rule.isActive());
    }

    private ScheduleDto toDto(AvailableSchedule schedule) {
        return new ScheduleDto(schedule.getId(), schedule.getCourt().getId(), schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime(), schedule.isActive());
    }

    private CourtPromotionDto toDto(CourtPromotion promotion) {
        return new CourtPromotionDto(
                promotion.getId(),
                promotion.getCourt().getId(),
                promotion.getName(),
                promotion.getType(),
                promotion.getFixedPrice(),
                promotion.getDiscountPercent(),
                promotion.getRequiredHours(),
                promotion.getStartTime(),
                promotion.getEndTime(),
                promotion.getValidFrom(),
                promotion.getValidTo(),
                promotion.isActive()
        );
    }
}
