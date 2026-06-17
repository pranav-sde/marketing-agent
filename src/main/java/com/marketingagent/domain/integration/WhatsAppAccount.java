package com.marketingagent.domain.integration;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "whatsapp_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_whatsapp_accounts_provider_business",
                        columnNames = {"provider_business_account_id"}
                ),
                @UniqueConstraint(
                        name = "uk_whatsapp_accounts_provider_phone",
                        columnNames = {"provider_phone_number_id"}
                )
        },
        indexes = {
                @Index(name = "idx_whatsapp_accounts_tenant", columnList = "tenant_id"),
                @Index(name = "idx_whatsapp_accounts_status", columnList = "status")
        }
)
public class WhatsAppAccount extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank
    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @NotBlank
    @Column(name = "provider_business_account_id", nullable = false, length = 128)
    private String providerBusinessAccountId;

    @NotBlank
    @Column(name = "provider_phone_number_id", nullable = false, length = 128)
    private String providerPhoneNumberId;

    @NotBlank
    @Column(name = "secret_ref", nullable = false, length = 512)
    private String secretRef;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WhatsAppAccountStatus status = WhatsAppAccountStatus.ACTIVE;

    protected WhatsAppAccount() {
    }

    public WhatsAppAccount(
            Tenant tenant,
            String displayName,
            String providerBusinessAccountId,
            String providerPhoneNumberId,
            String secretRef
    ) {
        this.tenant = tenant;
        this.displayName = displayName;
        this.providerBusinessAccountId = providerBusinessAccountId;
        this.providerPhoneNumberId = providerPhoneNumberId;
        this.secretRef = secretRef;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProviderBusinessAccountId() {
        return providerBusinessAccountId;
    }

    public String getProviderPhoneNumberId() {
        return providerPhoneNumberId;
    }

    public String getSecretRef() {
        return secretRef;
    }

    public void setSecretRef(String secretRef) {
        this.secretRef = secretRef;
    }

    public WhatsAppAccountStatus getStatus() {
        return status;
    }

    public void setStatus(WhatsAppAccountStatus status) {
        this.status = status;
    }
}
