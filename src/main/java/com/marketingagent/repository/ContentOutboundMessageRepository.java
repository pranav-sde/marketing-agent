package com.marketingagent.repository;

import com.marketingagent.domain.message.ContentOutboundMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentOutboundMessageRepository extends JpaRepository<ContentOutboundMessage, UUID> {
    
    Optional<ContentOutboundMessage> findByProviderMessageId(String providerMessageId);

    List<ContentOutboundMessage> findByCalendarEntry_Id(UUID calendarEntryId);

    List<ContentOutboundMessage> findByAdHocCampaign_Id(UUID adHocCampaignId);

    @Query("SELECT status, COUNT(id) FROM ContentOutboundMessage WHERE calendarEntry.id = :calendarEntryId GROUP BY status")
    List<Object[]> countStatusByCalendarEntryId(UUID calendarEntryId);

    @Query("SELECT status, COUNT(id) FROM ContentOutboundMessage WHERE adHocCampaign.id = :adHocCampaignId GROUP BY status")
    List<Object[]> countStatusByAdHocCampaignId(UUID adHocCampaignId);
}
