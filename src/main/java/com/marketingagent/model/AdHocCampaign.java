package com.marketingagent.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "adhoc_campaigns")
public class AdHocCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String messageText;

    @Column(length = 2000)
    private String mediaUrl;

    @Column(nullable = false)
    private String platform; // WHATSAPP, LINKEDIN

    @Column(nullable = false)
    private String status; // SCHEDULED, SENT, FAILED, ON_HOLD

    @Column(nullable = false)
    private Instant scheduledTime;

    private LocalDateTime sentAt;

    public AdHocCampaign() {}

    public AdHocCampaign(Tenant tenant, String messageText, String mediaUrl, String platform, String status, Instant scheduledTime) {
        this.tenant = tenant;
        this.messageText = messageText;
        this.mediaUrl = mediaUrl;
        this.platform = platform;
        this.status = status;
        this.scheduledTime = scheduledTime;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Instant scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
