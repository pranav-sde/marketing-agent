package com.marketingagent.domain.magazine;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;

/**
 * Stores the actual AI-generated content for a calendar entry on a specific platform.
 */
@Entity
@Table(
        name = "generated_content",
        indexes = {
                @Index(name = "idx_generated_content_calendar", columnList = "calendar_entry_id")
        }
)
public class GeneratedContent extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_entry_id", nullable = false)
    private ContentCalendar calendarEntry;

    @NotBlank
    @Column(name = "message_text", columnDefinition = "text", nullable = false)
    private String messageText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hashtags", columnDefinition = "jsonb")
    private List<String> hashtags;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private ContentPlatform platform;

    @Column(name = "media_url", length = 1024)
    private String mediaUrl;

    @Column(name = "groq_request_id", length = 255)
    private String groqRequestId;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "generated_at")
    private Instant generatedAt;

    protected GeneratedContent() {
    }

    public GeneratedContent(Tenant tenant, ContentCalendar calendarEntry, String messageText, ContentPlatform platform) {
        this.tenant = tenant;
        this.calendarEntry = calendarEntry;
        this.messageText = messageText;
        this.platform = platform;
        this.generatedAt = Instant.now();
    }

    public Tenant getTenant() { return tenant; }
    public ContentCalendar getCalendarEntry() { return calendarEntry; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public List<String> getHashtags() { return hashtags; }
    public void setHashtags(List<String> hashtags) { this.hashtags = hashtags; }

    public ContentPlatform getPlatform() { return platform; }
    public void setPlatform(ContentPlatform platform) { this.platform = platform; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getGroqRequestId() { return groqRequestId; }
    public void setGroqRequestId(String groqRequestId) { this.groqRequestId = groqRequestId; }

    public Integer getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(Integer tokenUsage) { this.tokenUsage = tokenUsage; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
