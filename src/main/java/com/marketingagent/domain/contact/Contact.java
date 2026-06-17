package com.marketingagent.domain.contact;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "contacts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contacts_tenant_phone",
                columnNames = {"tenant_id", "phone_e164"}
        ),
        indexes = {
                @Index(name = "idx_contacts_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_contacts_phone_hash", columnList = "phone_hash")
        }
)
public class Contact extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank
    @Column(name = "phone_e164", nullable = false, length = 32)
    private String phoneE164;

    @NotBlank
    @Column(name = "phone_hash", nullable = false, length = 128)
    private String phoneHash;

    @Column(name = "first_name", length = 120)
    private String firstName;

    @Column(name = "last_name", length = 120)
    private String lastName;

    @Column(name = "email", length = 254)
    private String email;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ContactStatus status = ContactStatus.ACTIVE;

    protected Contact() {
    }

    public Contact(Tenant tenant, String phoneE164, String phoneHash) {
        this.tenant = tenant;
        this.phoneE164 = phoneE164;
        this.phoneHash = phoneHash;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getPhoneE164() {
        return phoneE164;
    }

    public void setPhoneE164(String phoneE164) {
        this.phoneE164 = phoneE164;
    }

    public String getPhoneHash() {
        return phoneHash;
    }

    public void setPhoneHash(String phoneHash) {
        this.phoneHash = phoneHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ContactStatus getStatus() {
        return status;
    }

    public void setStatus(ContactStatus status) {
        this.status = status;
    }
}
