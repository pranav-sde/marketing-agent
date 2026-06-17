package com.marketingagent.repository;

import com.marketingagent.domain.user.Membership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    List<Membership> findByTenant_IdAndActiveTrue(UUID tenantId);

    List<Membership> findByUserAccount_IdAndActiveTrue(UUID userAccountId);

    Optional<Membership> findByTenant_IdAndUserAccount_Id(UUID tenantId, UUID userAccountId);
}
