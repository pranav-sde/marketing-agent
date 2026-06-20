package com.marketingagent.domain.message;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.contact.Contact;
import com.marketingagent.domain.magazine.ContentCalendar;
import com.marketingagent.domain.campaign.AdHocCampaign;
import com.marketingagent.domain.magazine.ContentPlatform;
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
        name = "content_outbound_messages",
        indexes = {
                @Index(name = "idx_content_out_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_content_out_calendar", columnList = "calendar_entry_id"),
                @Index(name = "idx_content_out_adhoc", columnList = "ad_hoc_campaign_id"),
                @Index(name = "idx_content_out_provider_id", columnList = "provider_message_id")
        }
)
public class ContentOutboundMessage extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_entry_id")
    private ContentCalendar calendarEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_hoc_campaign_id")
    private AdHocCampaign adHocCampaign;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 32)
    private ContentPlatform platform;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboundMessageStatus status = OutboundMessageStatus.PENDING;

    @Column(name = "provider_message_id", length = 160)
    private String providerMessageId;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected ContentOutboundMessage() {}

    public ContentOutboundMessage(Tenant tenant, Contact contact, ContentPlatform platform) {
        this.tenant = tenant;
        this.contact = contact;
        this.platform = platform;
    }

    public Tenant getTenant() { return tenant; }
    public ContentCalendar getCalendarEntry() { return calendarEntry; }
    public void setCalendarEntry(ContentCalendar calendarEntry) { this.calendarEntry = calendarEntry; }
    public AdHocCampaign getAdHocCampaign() { return adHocCampaign; }
    public void setAdHocCampaign(AdHocCampaign adHocCampaign) { this.adHocCampaign = adHocCampaign; }
    public Contact getContact() { return contact; }
    public ContentPlatform getPlatform() { return platform; }
    public OutboundMessageStatus getStatus() { return status; }
    public void setStatus(OutboundMessageStatus status) { this.status = status; }
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
