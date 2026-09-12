package com.thecodinganalyst.staffalias.tenant;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tenant_setting", uniqueConstraints = @UniqueConstraint(name = "uk_tenant_setting_tenant_key", columnNames = {"tenant_id", "setting_key"}))
public class TenantSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "setting_key", nullable = false, length = 100)
    private String key;

    @Column(name = "setting_value")
    private String value;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected TenantSetting() {
    }

    public TenantSetting(Tenant tenant, String key, String value) {
        this.tenant = tenant;
        this.key = key;
        this.value = value;
    }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getKey() { return key; }
    public String getValue() { return value; }

    public void updateValue(String value) {
        this.value = value;
        this.updatedAt = OffsetDateTime.now();
    }
}
