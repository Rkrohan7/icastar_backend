package com.icastar.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_settings")
@Data
@EqualsAndHashCode(exclude = {"updatedByUser"})
@ToString(exclude = {"updatedByUser"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", length = 1000)
    private String settingValue;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "updated_by")
    private Long updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", insertable = false, updatable = false)
    private User updatedByUser;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Category constants
    public static final String CATEGORY_LANDING_STATS = "LANDING_STATS";
    public static final String CATEGORY_GENERAL = "GENERAL";
    public static final String CATEGORY_REGISTRATION = "REGISTRATION";
    public static final String CATEGORY_JOB = "JOB";
    public static final String CATEGORY_NOTIFICATION = "NOTIFICATION";
}