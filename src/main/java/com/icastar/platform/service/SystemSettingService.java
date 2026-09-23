package com.icastar.platform.service;

import com.icastar.platform.entity.SystemSetting;
import com.icastar.platform.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SystemSettingService {

    private final SystemSettingRepository settingRepository;

    // Whitelist of allowed setting keys to prevent arbitrary key creation
    private static final Set<String> ALLOWED_KEYS = Set.of(
            // Landing stats
            "landingStatsEnabled",
            "landingActiveArtists",
            "landingCastingDirectors",
            "landingSuccessfulAuditions",
            "landingSuccessRate",
            "landingBlogsEnabled",
            // General settings
            "platformName",
            "platformEmail",
            "supportEmail",
            "supportPhone",
            "platformUrl",
            "maintenanceMode",
            "maintenanceMessage",
            // Registration settings
            "allowNewRegistrations",
            "requireEmailVerification",
            "requireMobileVerification",
            "otpExpirationMinutes",
            "otpLength",
            // Job settings
            "maxJobsPerRecruiter",
            "jobExpirationDays",
            // Notification settings
            "emailNotificationsEnabled",
            "smsNotificationsEnabled",
            "pushNotificationsEnabled"
    );

    // Default values for landing stats
    private static final Map<String, String> DEFAULT_VALUES = Map.of(
            "landingActiveArtists", "10000",
            "landingCastingDirectors", "250",
            "landingSuccessfulAuditions", "10000",
            "landingSuccessRate", "95"
    );

    /**
     * Get a setting value by key
     */
    @Transactional(readOnly = true)
    public Optional<String> getValue(String key) {
        return settingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue);
    }

    /**
     * Get a setting value with default fallback
     */
    @Transactional(readOnly = true)
    public String getValueOrDefault(String key, String defaultValue) {
        return getValue(key).orElse(defaultValue);
    }

    /**
     * Get integer value with default fallback
     */
    @Transactional(readOnly = true)
    public Integer getIntValueOrDefault(String key, Integer defaultValue) {
        return getValue(key)
                .map(v -> {
                    try {
                        return Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * Get boolean value with default fallback
     */
    @Transactional(readOnly = true)
    public Boolean getBooleanValueOrDefault(String key, Boolean defaultValue) {
        return getValue(key)
                .map(v -> "true".equalsIgnoreCase(v) || "1".equals(v))
                .orElse(defaultValue);
    }

    /**
     * Upsert a setting (insert if not exists, update if exists)
     * Only allowed keys can be saved
     */
    @CacheEvict(value = "landingStats", allEntries = true)
    public SystemSetting upsertSetting(String key, String value, String category, Long updatedBy) {
        log.info("Upserting setting: key={}, category={}, updatedBy={}", key, category, updatedBy);

        // Validate key is in whitelist
        if (!ALLOWED_KEYS.contains(key)) {
            throw new RuntimeException("Setting key '" + key + "' is not allowed");
        }

        SystemSetting setting = settingRepository.findBySettingKey(key)
                .orElseGet(() -> {
                    SystemSetting newSetting = new SystemSetting();
                    newSetting.setSettingKey(key);
                    return newSetting;
                });

        setting.setSettingValue(value);
        setting.setCategory(category);
        setting.setUpdatedBy(updatedBy);
        setting.setUpdatedAt(LocalDateTime.now());

        return settingRepository.save(setting);
    }

    /**
     * Get all settings by category
     */
    @Transactional(readOnly = true)
    public List<SystemSetting> getSettingsByCategory(String category) {
        return settingRepository.findByCategory(category);
    }

    /**
     * Get all settings as a map
     */
    @Transactional(readOnly = true)
    public Map<String, String> getAllSettingsAsMap() {
        Map<String, String> result = new HashMap<>();
        settingRepository.findAll().forEach(s ->
                result.put(s.getSettingKey(), s.getSettingValue()));
        return result;
    }

    /**
     * Get landing stats for public API (cached)
     * Includes 'enabled' field and all stats
     */
    @Cacheable(value = "landingStats", key = "'stats'")
    @Transactional(readOnly = true)
    public Map<String, Object> getLandingStats() {
        log.info("Fetching landing stats from database");

        Map<String, Object> stats = new HashMap<>();

        // Get enabled flag (default true)
        stats.put("enabled", getBooleanValueOrDefault("landingStatsEnabled", true));

        // Get blogs enabled flag (default true)
        stats.put("blogsEnabled", getBooleanValueOrDefault("landingBlogsEnabled", true));

        // Get values from DB with defaults
        stats.put("activeArtists", getIntValueOrDefault("landingActiveArtists",
                Integer.parseInt(DEFAULT_VALUES.get("landingActiveArtists"))));
        stats.put("castingDirectors", getIntValueOrDefault("landingCastingDirectors",
                Integer.parseInt(DEFAULT_VALUES.get("landingCastingDirectors"))));
        stats.put("successfulAuditions", getIntValueOrDefault("landingSuccessfulAuditions",
                Integer.parseInt(DEFAULT_VALUES.get("landingSuccessfulAuditions"))));
        stats.put("successRate", getIntValueOrDefault("landingSuccessRate",
                Integer.parseInt(DEFAULT_VALUES.get("landingSuccessRate"))));

        return stats;
    }

    /**
     * Get landing stats for admin config (with landing prefix)
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> getLandingStatsForAdmin() {
        Map<String, Integer> stats = new HashMap<>();

        stats.put("landingActiveArtists", getIntValueOrDefault("landingActiveArtists",
                Integer.parseInt(DEFAULT_VALUES.get("landingActiveArtists"))));
        stats.put("landingCastingDirectors", getIntValueOrDefault("landingCastingDirectors",
                Integer.parseInt(DEFAULT_VALUES.get("landingCastingDirectors"))));
        stats.put("landingSuccessfulAuditions", getIntValueOrDefault("landingSuccessfulAuditions",
                Integer.parseInt(DEFAULT_VALUES.get("landingSuccessfulAuditions"))));
        stats.put("landingSuccessRate", getIntValueOrDefault("landingSuccessRate",
                Integer.parseInt(DEFAULT_VALUES.get("landingSuccessRate"))));

        return stats;
    }

    /**
     * Check if a key is allowed
     */
    public boolean isKeyAllowed(String key) {
        return ALLOWED_KEYS.contains(key);
    }
}