package com.marketingagent.repository;

import com.marketingagent.domain.magazine.GeneratedContent;
import com.marketingagent.domain.magazine.ContentPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneratedContentRepository extends JpaRepository<GeneratedContent, UUID> {
    List<GeneratedContent> findByCalendarEntry_Id(UUID calendarEntryId);
    Optional<GeneratedContent> findByCalendarEntry_IdAndPlatform(UUID calendarEntryId, ContentPlatform platform);
    void deleteByCalendarEntry_Magazine_Id(UUID magazineId);
}
