package com.example.heimdall.project.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "prj_projects", uniqueConstraints = {
        @UniqueConstraint(name = "uk_prj_projects_owner_code", columnNames = {"owner_id", "code"})
})
public class ProjectJpaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "municipality_code", length = 5)
    private String municipalityCode;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "coordinates_source", nullable = false, length = 20)
    private String coordinatesSource;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "daily_run_time", nullable = false)
    private LocalTime dailyRunTime;

    @Column(name = "schedule_enabled", nullable = false)
    private boolean scheduleEnabled;

    @Column(name = "notify_lead_days", nullable = false)
    private short notifyLeadDays;

    @Column(name = "notify_min_level", nullable = false, length = 20)
    private String notifyMinLevel;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProjectJpaEntity() {}

    public ProjectJpaEntity(UUID id, UUID ownerId, String code, String name, String countryCode,
                            String municipalityCode, BigDecimal latitude, BigDecimal longitude,
                            String coordinatesSource, String timezone, LocalTime dailyRunTime,
                            boolean scheduleEnabled, short notifyLeadDays, String notifyMinLevel,
                            String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.code = code;
        this.name = name;
        this.countryCode = countryCode;
        this.municipalityCode = municipalityCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.coordinatesSource = coordinatesSource;
        this.timezone = timezone;
        this.dailyRunTime = dailyRunTime;
        this.scheduleEnabled = scheduleEnabled;
        this.notifyLeadDays = notifyLeadDays;
        this.notifyMinLevel = notifyMinLevel;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCountryCode() { return countryCode; }
    public String getMunicipalityCode() { return municipalityCode; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public String getCoordinatesSource() { return coordinatesSource; }
    public String getTimezone() { return timezone; }
    public LocalTime getDailyRunTime() { return dailyRunTime; }
    public boolean isScheduleEnabled() { return scheduleEnabled; }
    public short getNotifyLeadDays() { return notifyLeadDays; }
    public String getNotifyMinLevel() { return notifyMinLevel; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
