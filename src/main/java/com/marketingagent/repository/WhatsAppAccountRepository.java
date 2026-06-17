package com.marketingagent.repository;

import com.marketingagent.domain.integration.WhatsAppAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsAppAccountRepository extends JpaRepository<WhatsAppAccount, UUID> {
    List<WhatsAppAccount> findByTenant_Id(UUID tenantId);

    Optional<WhatsAppAccount> findByProviderPhoneNumberId(String providerPhoneNumberId);

    Optional<WhatsAppAccount> findByProviderBusinessAccountId(String providerBusinessAccountId);
}
