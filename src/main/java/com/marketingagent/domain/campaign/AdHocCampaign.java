package com.marketingagent.domain.campaign;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.magazine.ContentPlatform;
import com.marketingagent.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "ad_hoc_campaigns")
public class AdHocCampaign extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "message_text", length = 2000)
    private String messageText;

    @Column(name = "media_url", length = 1000)
    private String mediaUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 32)
    private ContentPlatform platform = ContentPlatform.WHATSAPP;

    @NotNull
    @Column(name = "scheduled_time", nullable = false)
    private Instant scheduledTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AdHocCampaignStatus status = AdHocCampaignStatus.SCHEDULED;

    protected AdHocCampaign() {}

    public AdHocCampaign(Tenant tenant, String messageText, String mediaUrl, ContentPlatform platform, Instant scheduledTime) {
        this.tenant = tenant;
        this.messageText = messageText;
        this.mediaUrl = mediaUrl;
        this.platform = platform;
        this.scheduledTime = scheduledTime;
    }

    public Tenant getTenant() { return tenant; }
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public ContentPlatform getPlatform() { return platform; }
    public Instant getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(Instant scheduledTime) { this.scheduledTime = scheduledTime; }
    public AdHocCampaignStatus getStatus() { return status; }
    public void setStatus(AdHocCampaignStatus status) { this.status = status; }
}
