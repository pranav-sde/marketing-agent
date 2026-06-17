package com.marketingagent.repository;

import com.marketingagent.domain.common.Channel;
import com.marketingagent.domain.contact.ContactChannelState;
import com.marketingagent.domain.contact.ContactStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactChannelStateRepository extends JpaRepository<ContactChannelState, UUID> {
    Optional<ContactChannelState> findByContact_IdAndChannel(UUID contactId, Channel channel);

    List<ContactChannelState> findByTenant_IdAndChannelAndStatus(UUID tenantId, Channel channel, ContactStatus status);
}
