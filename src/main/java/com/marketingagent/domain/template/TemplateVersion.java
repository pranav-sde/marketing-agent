package com.marketingagent.domain.template;

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
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(
        name = "template_versions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_template_versions_template_version",
                columnNames = {"message_template_id", "version_number"}
        ),
        indexes = {
                @Index(name = "idx_template_versions_tenant", columnList = "tenant_id"),
                @Index(name = "idx_template_versions_template", columnList = "message_template_id")
        }
)
public class TemplateVersion extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_template_id", nullable = false)
    private MessageTemplate messageTemplate;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "components", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> components = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variable_examples", columnDefinition = "jsonb")
    private Map<String, Object> variableExamples = new LinkedHashMap<>();

    @Column(name = "submitted_by_user_id")
    private java.util.UUID submittedByUserId;

    protected TemplateVersion() {
    }

    public TemplateVersion(
            Tenant tenant,
            MessageTemplate messageTemplate,
            int versionNumber,
            Map<String, Object> components
    ) {
        this.tenant = tenant;
        this.messageTemplate = messageTemplate;
        this.versionNumber = versionNumber;
        this.components = new LinkedHashMap<>(components);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public MessageTemplate getMessageTemplate() {
        return messageTemplate;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public Map<String, Object> getComponents() {
        return components;
    }

    public void setComponents(Map<String, Object> components) {
        this.components = new LinkedHashMap<>(components);
    }

    public Map<String, Object> getVariableExamples() {
        return variableExamples;
    }

    public void setVariableExamples(Map<String, Object> variableExamples) {
        this.variableExamples = new LinkedHashMap<>(variableExamples);
    }

    public java.util.UUID getSubmittedByUserId() {
        return submittedByUserId;
    }

    public void setSubmittedByUserId(java.util.UUID submittedByUserId) {
        this.submittedByUserId = submittedByUserId;
    }
}
