package com.marketingagent.domain.audience;

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
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(
        name = "segments",
        indexes = {
                @Index(name = "idx_segments_tenant_type", columnList = "tenant_id,type"),
                @Index(name = "idx_segments_name", columnList = "name")
        }
)
public class Segment extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank
    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private SegmentType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_definition", columnDefinition = "jsonb")
    private Map<String, Object> ruleDefinition = new LinkedHashMap<>();

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Segment() {
    }

    public Segment(Tenant tenant, String name, SegmentType type) {
        this.tenant = tenant;
        this.name = name;
        this.type = type;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SegmentType getType() {
        return type;
    }

    public Map<String, Object> getRuleDefinition() {
        return ruleDefinition;
    }

    public void setRuleDefinition(Map<String, Object> ruleDefinition) {
        this.ruleDefinition = new LinkedHashMap<>(ruleDefinition);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
