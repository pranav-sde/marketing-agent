package com.marketingagent.domain.message;

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
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(
        name = "delivery_events",
        indexes = {
                @Index(name = "idx_delivery_events_tenant_type", columnList = "tenant_id,event_type"),
                @Index(name = "idx_delivery_events_message", columnList = "outbound_message_id"),
                @Index(name = "idx_delivery_events_time", columnList = "event_time")
        }
)
public class DeliveryEvent extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_message_id")
    private OutboundMessage outboundMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_webhook_event_id")
    private ProviderWebhookEvent providerWebhookEvent;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private DeliveryEventType eventType;

    @NotNull
    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "provider_error_code", length = 120)
    private String providerErrorCode;

    protected DeliveryEvent() {
    }

    public DeliveryEvent(Tenant tenant, DeliveryEventType eventType, Instant eventTime) {
        this.tenant = tenant;
        this.eventType = eventType;
        this.eventTime = eventTime;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public OutboundMessage getOutboundMessage() {
        return outboundMessage;
    }

    public void setOutboundMessage(OutboundMessage outboundMessage) {
        this.outboundMessage = outboundMessage;
    }

    public ProviderWebhookEvent getProviderWebhookEvent() {
        return providerWebhookEvent;
    }

    public void setProviderWebhookEvent(ProviderWebhookEvent providerWebhookEvent) {
        this.providerWebhookEvent = providerWebhookEvent;
    }

    public DeliveryEventType getEventType() {
        return eventType;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public String getProviderErrorCode() {
        return providerErrorCode;
    }

    public void setProviderErrorCode(String providerErrorCode) {
        this.providerErrorCode = providerErrorCode;
    }
}
