package com.resergrass.config;

import com.resergrass.domain.entity.AvailableSchedule;
import com.resergrass.domain.entity.Court;
import com.resergrass.domain.entity.CourtPriceRule;
import com.resergrass.domain.entity.User;
import com.resergrass.domain.enums.CourtStatus;
import com.resergrass.domain.enums.CourtType;
import com.resergrass.domain.enums.PriceDayType;
import com.resergrass.domain.enums.Role;
import com.resergrass.repository.AvailableScheduleRepository;
import com.resergrass.repository.CourtPriceRuleRepository;
import com.resergrass.repository.CourtRepository;
import com.resergrass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    @Bean
    CommandLineRunner createDefaultData(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CourtRepository courtRepository,
            AvailableScheduleRepository scheduleRepository,
            CourtPriceRuleRepository priceRuleRepository,
            JdbcTemplate jdbcTemplate
    ) {
        return args -> {
            migrateLegacyCourtPrices(jdbcTemplate);
            migrateGuestReservations(jdbcTemplate);
            migratePaymentWorkflow(jdbcTemplate);
            if (!userRepository.existsByEmail("admin@resergrass.com")) {
                var admin = new User();
                admin.setFullName("Administrador");
                admin.setEmail("admin@resergrass.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                log.info("DATA_INIT_ADMIN_CREATED email={}", admin.getEmail());
            } else {
                log.info("DATA_INIT_ADMIN_EXISTS email=admin@resergrass.com");
            }
            if (courtRepository.count() == 0) {
                seedCourt("Cancha 1", "C-001", courtRepository, scheduleRepository, priceRuleRepository);
                seedCourt("Cancha 2", "C-002", courtRepository, scheduleRepository, priceRuleRepository);
                log.info("DATA_INIT_COURTS_CREATED count=2");
            } else {
                log.info("DATA_INIT_COURTS_EXISTS count={}", courtRepository.count());
            }
        };
    }

    private void seedCourt(
            String name,
            String code,
            CourtRepository courtRepository,
            AvailableScheduleRepository scheduleRepository,
            CourtPriceRuleRepository priceRuleRepository
    ) {
        var court = new Court();
        court.setName(name);
        court.setCode(code);
        court.setDescription("Cancha de grass sintético para reservas por hora.");
        court.setMainImageUrl("https://images.unsplash.com/photo-1624880357913-a8539238245b?auto=format&fit=crop&w=900&q=80");
        court.setType(CourtType.GRASS_SINTETICO);
        court.setDimensions("40 x 20 m");
        court.setMaxPlayers(10);
        court.setStatus(CourtStatus.DISPONIBLE);
        var saved = courtRepository.save(court);
        log.info("DATA_INIT_COURT_SEED id={} name={} code={}", saved.getId(), saved.getName(), saved.getCode());

        for (DayOfWeek day : DayOfWeek.values()) {
            var schedule = new AvailableSchedule();
            schedule.setCourt(saved);
            schedule.setDayOfWeek(day);
            schedule.setStartTime(LocalTime.of(day == DayOfWeek.SUNDAY ? 8 : 7, 0));
            schedule.setEndTime(LocalTime.of(23, 0));
            scheduleRepository.save(schedule);
        }

        List.of(
                priceRule(PriceDayType.WEEKDAY, LocalTime.of(8, 0), LocalTime.of(17, 0), "60.00"),
                priceRule(PriceDayType.WEEKDAY, LocalTime.of(17, 0), LocalTime.of(23, 0), "80.00"),
                priceRule(PriceDayType.WEEKEND, LocalTime.of(8, 0), LocalTime.of(23, 0), "100.00")
        ).forEach(rule -> {
            rule.setCourt(saved);
            priceRuleRepository.save(rule);
        });
    }

    private CourtPriceRule priceRule(PriceDayType dayType, LocalTime start, LocalTime end, String price) {
        var rule = new CourtPriceRule();
        rule.setDayType(dayType);
        rule.setStartTime(start);
        rule.setEndTime(end);
        rule.setHourlyPrice(new BigDecimal(price));
        return rule;
    }

    private void migrateLegacyCourtPrices(JdbcTemplate jdbcTemplate) {
        try {
            var hasLegacyColumn = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                    select exists (
                        select 1
                        from information_schema.columns
                        where table_name = 'courts'
                          and column_name = 'hourly_price'
                    )
                    """, Boolean.class));
            if (!hasLegacyColumn) {
                log.info("DATA_INIT_LEGACY_PRICE_MIGRATION_SKIPPED reason=hourly_price_column_absent");
                return;
            }
            jdbcTemplate.execute("""
                    insert into court_price_rules (court_id, day_type, start_time, end_time, hourly_price, active)
                    select id, 'WEEKDAY', time '08:00', time '23:00', hourly_price, true
                    from courts c
                    where hourly_price is not null
                      and not exists (select 1 from court_price_rules r where r.court_id = c.id)
                    """);
            jdbcTemplate.execute("alter table courts drop column if exists hourly_price");
            log.info("DATA_INIT_LEGACY_PRICE_MIGRATION_DONE");
        } catch (Exception ignored) {
            log.warn("DATA_INIT_LEGACY_PRICE_MIGRATION_FAILED reason={}", ignored.getMessage());
            // Startup should not fail if the schema is already migrated or the table is not created yet.
        }
    }

    private void migrateGuestReservations(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.execute("alter table reservations alter column client_id drop not null");
            jdbcTemplate.execute("alter table reservations add column if not exists guest_name varchar(120)");
            jdbcTemplate.execute("alter table reservations add column if not exists guest_phone varchar(30)");
            log.info("DATA_INIT_GUEST_RESERVATION_MIGRATION_DONE");
        } catch (Exception ex) {
            log.warn("DATA_INIT_GUEST_RESERVATION_MIGRATION_FAILED reason={}", ex.getMessage());
        }
    }

    private void migratePaymentWorkflow(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.execute("alter table reservations add column if not exists payment_expires_at timestamp with time zone");
            jdbcTemplate.execute("alter table reservations add column if not exists cancelled_at timestamp with time zone");
            jdbcTemplate.execute("alter table payments add column if not exists rejection_reason varchar(300)");
            jdbcTemplate.execute("alter table payments add column if not exists operation_number varchar(80)");
            jdbcTemplate.execute("alter table payments drop constraint if exists payments_status_check");
            jdbcTemplate.execute("update payments set status = 'PENDIENTE_PAGO' where status = 'PENDIENTE'");
            jdbcTemplate.execute("update payments set status = 'EN_REVISION' where status = 'ADELANTO'");
            jdbcTemplate.execute("""
                    alter table payments add constraint payments_status_check
                    check (status in ('PENDIENTE_PAGO', 'EN_REVISION', 'PAGO_EN_LOCAL', 'RECHAZADO', 'PAGADO'))
                    """);
            log.info("DATA_INIT_PAYMENT_WORKFLOW_MIGRATION_DONE");
        } catch (Exception ex) {
            log.warn("DATA_INIT_PAYMENT_WORKFLOW_MIGRATION_FAILED reason={}", ex.getMessage());
        }
    }
}
