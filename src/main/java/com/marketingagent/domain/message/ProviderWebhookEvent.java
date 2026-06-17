package com.marketingagent.domain.message;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.common.ProviderType;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(
        name = "provider_webhook_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_provider_webhook_events_hash",
                columnNames = "event_hash"
        ),
        indexes = {
                @Index(name = "idx_provider_webhook_events_tenant", columnList = "tenant_id"),
                @Index(name = "idx_provider_webhook_events_provider_id", columnList = "provider_event_id"),
                @Index(name = "idx_provider_webhook_events_received", columnList = "received_at")
        }
)
public class ProviderWebhookEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "whatsapp_account_id")
    private WhatsAppAccount whatsAppAccount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 48)
    private ProviderType provider;

    @Column(name = "provider_event_id", length = 180)
    private String providerEventId;

    @NotBlank
    @Column(name = "event_hash", nullable = false, length = 128)
    private String eventHash;

    @NotNull
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = new LinkedHashMap<>();

    @Column(name = "processed_at")
    private Instant processedAt;

    protected ProviderWebhookEvent() {
    }

    public ProviderWebhookEvent(ProviderType provider, String eventHash, Instant receivedAt, Map<String, Object> payload) {
        this.provider = provider;
        this.eventHash = eventHash;
        this.receivedAt = receivedAt;
        this.payload = new LinkedHashMap<>(payload);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public WhatsAppAccount getWhatsAppAccount() {
        return whatsAppAccount;
    }

    public void setWhatsAppAccount(WhatsAppAccount whatsAppAccount) {
        this.whatsAppAccount = whatsAppAccount;
    }

    public ProviderType getProvider() {
        return provider;
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    public void setProviderEventId(String providerEventId) {
        this.providerEventId = providerEventId;
    }

    public String getEventHash() {
        return eventHash;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
