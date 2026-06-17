package com.marketingagent.domain.broadcast;

import com.marketingagent.domain.audience.AudienceSnapshot;
import com.marketingagent.domain.campaign.Campaign;
import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.integration.WhatsAppAccount;
import com.marketingagent.domain.template.MessageTemplate;
import com.marketingagent.domain.template.TemplateVersion;
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
        name = "broadcasts",
        indexes = {
                @Index(name = "idx_broadcasts_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_broadcasts_campaign", columnList = "campaign_id"),
                @Index(name = "idx_broadcasts_scheduled", columnList = "scheduled_start_at")
        }
)
public class Broadcast extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audience_snapshot_id", nullable = false)
    private AudienceSnapshot audienceSnapshot;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_template_id", nullable = false)
    private MessageTemplate messageTemplate;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_version_id", nullable = false)
    private TemplateVersion templateVersion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "whatsapp_account_id", nullable = false)
    private WhatsAppAccount whatsAppAccount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BroadcastStatus status = BroadcastStatus.DRAFT;

    @Column(name = "scheduled_start_at")
    private Instant scheduledStartAt;

    @Column(name = "scheduled_end_at")
    private Instant scheduledEndAt;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "UTC";

    @Column(name = "rate_limit_profile", nullable = false, length = 80)
    private String rateLimitProfile = "standard";

    @Column(name = "locked_at")
    private Instant lockedAt;

    protected Broadcast() {
    }

    public Broadcast(
            Tenant tenant,
            Campaign campaign,
            AudienceSnapshot audienceSnapshot,
            MessageTemplate messageTemplate,
            TemplateVersion templateVersion,
            WhatsAppAccount whatsAppAccount
    ) {
        this.tenant = tenant;
        this.campaign = campaign;
        this.audienceSnapshot = audienceSnapshot;
        this.messageTemplate = messageTemplate;
        this.templateVersion = templateVersion;
        this.whatsAppAccount = whatsAppAccount;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public AudienceSnapshot getAudienceSnapshot() {
        return audienceSnapshot;
    }

    public MessageTemplate getMessageTemplate() {
        return messageTemplate;
    }

    public TemplateVersion getTemplateVersion() {
        return templateVersion;
    }

    public WhatsAppAccount getWhatsAppAccount() {
        return whatsAppAccount;
    }

    public BroadcastStatus getStatus() {
        return status;
    }

    public void setStatus(BroadcastStatus status) {
        this.status = status;
    }

    public Instant getScheduledStartAt() {
        return scheduledStartAt;
    }

    public void setScheduledStartAt(Instant scheduledStartAt) {
        this.scheduledStartAt = scheduledStartAt;
    }

    public Instant getScheduledEndAt() {
        return scheduledEndAt;
    }

    public void setScheduledEndAt(Instant scheduledEndAt) {
        this.scheduledEndAt = scheduledEndAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getRateLimitProfile() {
        return rateLimitProfile;
    }

    public void setRateLimitProfile(String rateLimitProfile) {
        this.rateLimitProfile = rateLimitProfile;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }
}
