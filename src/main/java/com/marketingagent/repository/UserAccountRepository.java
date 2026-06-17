package com.marketingagent.repository;

import com.marketingagent.domain.user.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByIdentityProviderSubject(String identityProviderSubject);
}
