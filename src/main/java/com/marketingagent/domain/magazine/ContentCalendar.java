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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Represents a single day in a 30-day content calendar, derived from a magazine story.
 */
@Entity
@Table(
        name = "content_calendar",
        indexes = {
                @Index(name = "idx_content_calendar_magazine", columnList = "magazine_id"),
                @Index(name = "idx_content_calendar_date_status", columnList = "scheduled_date,status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_magazine_day", columnNames = {"magazine_id", "day_number"})
        }
)
public class ContentCalendar extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "magazine_id", nullable = false)
    private Magazine magazine;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @NotNull
    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "content_angle", length = 255)
    private String contentAngle;

    @NotNull
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ContentCalendarStatus status = ContentCalendarStatus.PENDING;

    protected ContentCalendar() {
    }

    public ContentCalendar(Tenant tenant, Magazine magazine, Story story, Integer dayNumber, LocalDate scheduledDate) {
        this.tenant = tenant;
        this.magazine = magazine;
        this.story = story;
        this.dayNumber = dayNumber;
        this.scheduledDate = scheduledDate;
    }

    public Tenant getTenant() { return tenant; }
    public Magazine getMagazine() { return magazine; }
    public Story getStory() { return story; }

    public Integer getDayNumber() { return dayNumber; }
    public void setDayNumber(Integer dayNumber) { this.dayNumber = dayNumber; }

    public String getContentAngle() { return contentAngle; }
    public void setContentAngle(String contentAngle) { this.contentAngle = contentAngle; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public ContentCalendarStatus getStatus() { return status; }
    public void setStatus(ContentCalendarStatus status) { this.status = status; }
}
