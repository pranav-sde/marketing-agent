package com.marketingagent.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "calendar_entries")
public class CalendarEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id", nullable = false)
    private Magazine magazine;

    @Column(nullable = false)
    private int dayNumber;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    @Column(nullable = false)
    private String status; // PENDING, APPROVED, ON_HOLD, SENT, FAILED

    private String contentAngle;

    @Column(columnDefinition = "TEXT")
    private String messageText;

    @Column(length = 2000)
    private String mediaUrl;

    private LocalDateTime sentAt;

    public CalendarEntry() {}

    public CalendarEntry(Tenant tenant, Magazine magazine, int dayNumber, LocalDate scheduledDate, String status, String contentAngle, String messageText, String mediaUrl) {
        this.tenant = tenant;
        this.magazine = magazine;
        this.dayNumber = dayNumber;
        this.scheduledDate = scheduledDate;
        this.status = status;
        this.contentAngle = contentAngle;
        this.messageText = messageText;
        this.mediaUrl = mediaUrl;
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

    public Magazine getMagazine() {
        return magazine;
    }

    public void setMagazine(Magazine magazine) {
        this.magazine = magazine;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContentAngle() {
        return contentAngle;
    }

    public void setContentAngle(String contentAngle) {
        this.contentAngle = contentAngle;
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

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
