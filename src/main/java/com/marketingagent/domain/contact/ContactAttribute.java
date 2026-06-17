package com.marketingagent.domain.contact;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "contact_attributes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contact_attributes_contact_key",
                columnNames = {"contact_id", "attribute_key"}
        ),
        indexes = {
                @Index(name = "idx_contact_attributes_tenant_key", columnList = "tenant_id,attribute_key"),
                @Index(name = "idx_contact_attributes_contact", columnList = "contact_id")
        }
)
public class ContactAttribute extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @NotBlank
    @Column(name = "attribute_key", nullable = false, length = 120)
    private String key;

    @Column(name = "attribute_value", columnDefinition = "text")
    private String value;

    protected ContactAttribute() {
    }

    public ContactAttribute(Tenant tenant, Contact contact, String key, String value) {
        this.tenant = tenant;
        this.contact = contact;
        this.key = key;
        this.value = value;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Contact getContact() {
        return contact;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
