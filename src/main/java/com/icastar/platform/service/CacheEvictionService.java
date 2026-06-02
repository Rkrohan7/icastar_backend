package com.icastar.platform.service;

import com.icastar.platform.config.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Service for cross-service cache eviction.
 * Provides centralized cache management for complex scenarios
 * where multiple caches need to be invalidated together.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheEvictionService {

    private final CacheManager cacheManager;

    /**
     * Evict all caches - Use with caution, only for admin operations
     */
    public void evictAllCaches() {
        log.info("Evicting ALL caches");
        cacheManager.getCacheNames().forEach(cacheName -> {
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
            log.debug("Cleared cache: {}", cacheName);
        });
        log.info("All caches cleared successfully");
    }

    /**
     * Evict job-related caches when jobs are modified
     */
    @Caching(evict = {
        @CacheEvict(value = CacheNames.JOBS_ACTIVE, allEntries = true),
        @CacheEvict(value = CacheNames.JOBS_FEATURED, allEntries = true),
        @CacheEvict(value = CacheNames.JOBS_BY_ID, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_RECRUITER, allEntries = true)
    })
    public void evictJobCaches() {
        log.info("Job caches evicted");
    }

    /**
     * Evict a specific job from cache
     */
    public void evictJobById(Long jobId) {
        log.info("Evicting cache for job ID: {}", jobId);
        Objects.requireNonNull(cacheManager.getCache(CacheNames.JOBS_BY_ID)).evict(jobId);
    }

    /**
     * Evict user-related caches when user data is modified
     */
    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_BY_ID, allEntries = true),
        @CacheEvict(value = CacheNames.USER_BY_EMAIL, allEntries = true),
        @CacheEvict(value = CacheNames.USERS_BY_ROLE, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    })
    public void evictUserCaches() {
        log.info("User caches evicted");
    }

    /**
     * Evict a specific user from cache
     */
    public void evictUserById(Long userId) {
        log.info("Evicting cache for user ID: {}", userId);
        Objects.requireNonNull(cacheManager.getCache(CacheNames.USER_BY_ID)).evict(userId);
    }

    /**
     * Evict user by email from cache
     */
    public void evictUserByEmail(String email) {
        log.info("Evicting cache for user email: {}", email);
        Objects.requireNonNull(cacheManager.getCache(CacheNames.USER_BY_EMAIL)).evict(email);
    }

    /**
     * Evict artist type and field caches
     */
    @Caching(evict = {
        @CacheEvict(value = CacheNames.ARTIST_TYPES, allEntries = true),
        @CacheEvict(value = CacheNames.ARTIST_TYPE_FIELDS, allEntries = true)
    })
    public void evictArtistTypeCaches() {
        log.info("Artist type caches evicted");
    }

    /**
     * Evict dashboard caches for a specific recruiter
     */
    public void evictRecruiterDashboard(Long recruiterId) {
        log.info("Evicting dashboard cache for recruiter: {}", recruiterId);
        Objects.requireNonNull(cacheManager.getCache(CacheNames.DASHBOARD_RECRUITER)).evict(recruiterId);
        Objects.requireNonNull(cacheManager.getCache(CacheNames.DASHBOARD_RECRUITER)).evict("metrics-" + recruiterId);
    }

    /**
     * Evict dashboard caches for a specific artist
     */
    public void evictArtistDashboard(Long artistId) {
        log.info("Evicting dashboard cache for artist: {}", artistId);
        Objects.requireNonNull(cacheManager.getCache(CacheNames.DASHBOARD_ARTIST)).evict(artistId);
        Objects.requireNonNull(cacheManager.getCache(CacheNames.DASHBOARD_ARTIST)).evict("completion-" + artistId);
        Objects.requireNonNull(cacheManager.getCache(CacheNames.DASHBOARD_ARTIST)).evict("app-status-" + artistId);
    }

    /**
     * Evict all dashboard caches (for admin operations)
     */
    @Caching(evict = {
        @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_RECRUITER, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_ARTIST, allEntries = true)
    })
    public void evictAllDashboardCaches() {
        log.info("All dashboard caches evicted");
    }

    /**
     * Evict application-related caches
     */
    @Caching(evict = {
        @CacheEvict(value = CacheNames.JOB_APPLICATIONS, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_RECRUITER, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_ARTIST, allEntries = true)
    })
    public void evictApplicationCaches() {
        log.info("Application caches evicted");
    }

    /**
     * Evict casting call caches
     */
    @Caching(evict = {
        @CacheEvict(value = CacheNames.CASTING_CALLS, allEntries = true),
        @CacheEvict(value = CacheNames.CASTING_CALLS_OPEN, allEntries = true),
        @CacheEvict(value = CacheNames.AUDITIONS, allEntries = true)
    })
    public void evictCastingCallCaches() {
        log.info("Casting call caches evicted");
    }

    /**
     * Evict skills list cache
     */
    @CacheEvict(value = CacheNames.SKILLS_LIST, allEntries = true)
    public void evictSkillsCache() {
        log.info("Skills cache evicted");
    }

    /**
     * Evict artist-related caches
     */
    @Caching(evict = {
        @CacheEvict(value = CacheNames.ARTISTS_BY_TYPE, allEntries = true),
        @CacheEvict(value = CacheNames.ARTISTS_FEATURED, allEntries = true),
        @CacheEvict(value = CacheNames.ARTISTS_ACTIVE, allEntries = true)
    })
    public void evictArtistCaches() {
        log.info("Artist caches evicted");
    }

    /**
     * Get cache statistics - useful for monitoring
     */
    public void logCacheStats() {
        log.info("=== CACHE STATUS ===");
        cacheManager.getCacheNames().forEach(cacheName -> {
            log.info("Cache: {} - Active", cacheName);
        });
        log.info("====================");
    }
}
