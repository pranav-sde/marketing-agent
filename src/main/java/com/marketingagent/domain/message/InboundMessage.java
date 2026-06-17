package com.marketingagent.domain.message;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.contact.Contact;
import com.marketingagent.domain.integration.WhatsAppAccount;
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
        name = "inbound_messages",
        indexes = {
                @Index(name = "idx_inbound_messages_tenant_classification", columnList = "tenant_id,classification"),
                @Index(name = "idx_inbound_messages_contact", columnList = "contact_id"),
                @Index(name = "idx_inbound_messages_received", columnList = "received_at")
        }
)
public class InboundMessage extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "whatsapp_account_id", nullable = false)
    private WhatsAppAccount whatsAppAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_webhook_event_id")
    private ProviderWebhookEvent providerWebhookEvent;

    @Column(name = "provider_message_id", length = 160)
    private String providerMessageId;

    @Column(name = "from_phone_hash", nullable = false, length = 128)
    private String fromPhoneHash;

    @Column(name = "message_text", columnDefinition = "text")
    private String messageText;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false, length = 48)
    private InboundMessageClassification classification = InboundMessageClassification.UNKNOWN;

    @NotNull
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected InboundMessage() {
    }

    public InboundMessage(Tenant tenant, WhatsAppAccount whatsAppAccount, String fromPhoneHash, Instant receivedAt) {
        this.tenant = tenant;
        this.whatsAppAccount = whatsAppAccount;
        this.fromPhoneHash = fromPhoneHash;
        this.receivedAt = receivedAt;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public WhatsAppAccount getWhatsAppAccount() {
        return whatsAppAccount;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public ProviderWebhookEvent getProviderWebhookEvent() {
        return providerWebhookEvent;
    }

    public void setProviderWebhookEvent(ProviderWebhookEvent providerWebhookEvent) {
        this.providerWebhookEvent = providerWebhookEvent;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getFromPhoneHash() {
        return fromPhoneHash;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public InboundMessageClassification getClassification() {
        return classification;
    }

    public void setClassification(InboundMessageClassification classification) {
        this.classification = classification;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
