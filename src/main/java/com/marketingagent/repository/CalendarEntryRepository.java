package com.marketingagent.repository;

import com.marketingagent.model.CalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CalendarEntryRepository extends JpaRepository<CalendarEntry, UUID> {
    List<CalendarEntry> findByTenantIdAndMagazineIdOrderByDayNumberAsc(UUID tenantId, UUID magazineId);
    List<CalendarEntry> findByStatusAndScheduledDateLessThanEqual(String status, LocalDate date);
}
