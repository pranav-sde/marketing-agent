package com.marketingagent.domain.tenant;

import com.marketingagent.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;

@Entity
@Table(
        name = "tenants",
        uniqueConstraints = @UniqueConstraint(name = "uk_tenants_slug", columnNames = "slug"),
        indexes = @Index(name = "idx_tenants_status", columnList = "status")
)
public class Tenant extends BaseEntity {

    @NotBlank
    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @NotBlank
    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TenantStatus status = TenantStatus.ACTIVE;

    @NotBlank
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = ZoneId.of("UTC").getId();

    @Column(name = "default_locale", nullable = false, length = 16)
    private String defaultLocale = "en";

    @Column(name = "whatsapp_access_token", length = 512)
    private String whatsappAccessToken;

    @Column(name = "whatsapp_phone_number_id", length = 120)
    private String whatsappPhoneNumberId;

    protected Tenant() {
    }

    public Tenant(String name, String slug, String timezone, String defaultLocale) {
        this.name = name;
        this.slug = slug;
        this.timezone = timezone;
        this.defaultLocale = defaultLocale;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public String getWhatsappAccessToken() {
        return whatsappAccessToken;
    }

    public void setWhatsappAccessToken(String whatsappAccessToken) {
        this.whatsappAccessToken = whatsappAccessToken;
    }

    public String getWhatsappPhoneNumberId() {
        return whatsappPhoneNumberId;
    }

    public void setWhatsappPhoneNumberId(String whatsappPhoneNumberId) {
        this.whatsappPhoneNumberId = whatsappPhoneNumberId;
    }
}
