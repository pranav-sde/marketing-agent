package com.marketingagent.repository;

import com.marketingagent.domain.common.Channel;
import com.marketingagent.domain.contact.ConsentEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentEventRepository extends JpaRepository<ConsentEvent, UUID> {
    List<ConsentEvent> findByTenant_IdAndContact_IdOrderByCapturedAtDesc(UUID tenantId, UUID contactId);

    List<ConsentEvent> findByTenant_IdAndContact_IdAndChannelOrderByCapturedAtDesc(
            UUID tenantId,
            UUID contactId,
            Channel channel
    );
}
