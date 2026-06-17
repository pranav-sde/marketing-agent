package com.marketingagent.domain.user;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.tenant.Tenant;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "memberships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_memberships_tenant_user",
                columnNames = {"tenant_id", "user_account_id"}
        ),
        indexes = {
                @Index(name = "idx_memberships_tenant", columnList = "tenant_id"),
                @Index(name = "idx_memberships_user", columnList = "user_account_id")
        }
)
public class Membership extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "membership_roles",
            joinColumns = @JoinColumn(name = "membership_id"),
            indexes = @Index(name = "idx_membership_roles_membership", columnList = "membership_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 48)
    private Set<MembershipRole> roles = new LinkedHashSet<>();

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Membership() {
    }

    public Membership(Tenant tenant, UserAccount userAccount, Set<MembershipRole> roles) {
        this.tenant = tenant;
        this.userAccount = userAccount;
        this.roles = new LinkedHashSet<>(roles);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public Set<MembershipRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<MembershipRole> roles) {
        this.roles = new LinkedHashSet<>(roles);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
