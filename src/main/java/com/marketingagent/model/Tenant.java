package com.marketingagent.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String timezone;
    private String defaultLocale;

    @Column(length = 1000)
    private String whatsappAccessToken;

    private String whatsappPhoneNumberId;

    public Tenant() {}

    public Tenant(String name, String slug, String timezone, String defaultLocale) {
        this.name = name;
        this.slug = slug;
        this.timezone = timezone;
        this.defaultLocale = defaultLocale;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
