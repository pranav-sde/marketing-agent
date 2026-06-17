package com.marketingagent.domain.template;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.integration.WhatsAppAccount;
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
import java.time.Instant;

@Entity
@Table(
        name = "message_templates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_message_templates_account_name_language",
                columnNames = {"whatsapp_account_id", "name", "language"}
        ),
        indexes = {
                @Index(name = "idx_message_templates_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_message_templates_provider_id", columnList = "provider_template_id")
        }
)
public class MessageTemplate extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "whatsapp_account_id", nullable = false)
    private WhatsAppAccount whatsAppAccount;

    @NotBlank
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @NotBlank
    @Column(name = "language", nullable = false, length = 16)
    private String language;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private TemplateCategory category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TemplateStatus status = TemplateStatus.DRAFT;

    @Column(name = "provider_template_id", length = 160)
    private String providerTemplateId;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected MessageTemplate() {
    }

    public MessageTemplate(
            Tenant tenant,
            WhatsAppAccount whatsAppAccount,
            String name,
            String language,
            TemplateCategory category
    ) {
        this.tenant = tenant;
        this.whatsAppAccount = whatsAppAccount;
        this.name = name;
        this.language = language;
        this.category = category;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public WhatsAppAccount getWhatsAppAccount() {
        return whatsAppAccount;
    }

    public String getName() {
        return name;
    }

    public String getLanguage() {
        return language;
    }

    public TemplateCategory getCategory() {
        return category;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public void setStatus(TemplateStatus status) {
        this.status = status;
    }

    public String getProviderTemplateId() {
        return providerTemplateId;
    }

    public void setProviderTemplateId(String providerTemplateId) {
        this.providerTemplateId = providerTemplateId;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
