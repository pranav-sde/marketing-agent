package com.marketingagent.repository;

import com.marketingagent.domain.common.Channel;
import com.marketingagent.domain.contact.SuppressionListEntry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuppressionListEntryRepository extends JpaRepository<SuppressionListEntry, UUID> {
    Optional<SuppressionListEntry> findByTenant_IdAndChannelAndPhoneHash(
            UUID tenantId,
            Channel channel,
            String phoneHash
    );

    boolean existsByTenant_IdAndChannelAndPhoneHash(UUID tenantId, Channel channel, String phoneHash);
}
