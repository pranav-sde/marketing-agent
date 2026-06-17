package com.marketingagent.domain.user;

import com.marketingagent.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "user_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_accounts_email", columnNames = "email"),
        indexes = @Index(name = "idx_user_accounts_status", columnList = "status")
)
public class UserAccount extends BaseEntity {

    @Email
    @NotBlank
    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @NotBlank
    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status = UserStatus.INVITED;

    @Column(name = "identity_provider_subject", length = 200)
    private String identityProviderSubject;

    protected UserAccount() {
    }

    public UserAccount(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getIdentityProviderSubject() {
        return identityProviderSubject;
    }

    public void setIdentityProviderSubject(String identityProviderSubject) {
        this.identityProviderSubject = identityProviderSubject;
    }
}
