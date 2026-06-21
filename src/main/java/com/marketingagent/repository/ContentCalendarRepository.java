package com.marketingagent.repository;

import com.marketingagent.domain.magazine.ContentCalendar;
import com.marketingagent.domain.magazine.ContentCalendarStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContentCalendarRepository extends JpaRepository<ContentCalendar, UUID> {
    List<ContentCalendar> findByMagazine_IdOrderByDayNumberAsc(UUID magazineId);
    List<ContentCalendar> findByScheduledDateAndStatus(LocalDate date, ContentCalendarStatus status);
    void deleteByMagazine_Id(UUID magazineId);
}
