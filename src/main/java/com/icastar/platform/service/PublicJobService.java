package com.icastar.platform.service;

import com.icastar.platform.config.CacheNames;
import com.icastar.platform.dto.job.PublicJobDto;
import com.icastar.platform.entity.Job;
import com.icastar.platform.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for public job operations.
 * This service caches PublicJobDto (plain POJO) instead of Job entity
 * to avoid Hibernate proxy serialization issues in Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublicJobService {

    private final JobRepository jobRepository;

    /**
     * Find public job details by ID.
     * Returns a DTO that is safe to cache in Redis (no Hibernate entities).
     *
     * @param id Job ID
     * @return Optional<PublicJobDto> - cached DTO
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PUBLIC_JOBS_BY_ID, key = "#id", unless = "#result == null")
    public PublicJobDto findPublicJobById(Long id) {
        log.debug("Cache MISS: Loading public job DTO for id: {}", id);

        Optional<Job> jobOptional = jobRepository.findById(id);
        if (jobOptional.isEmpty()) {
            return null;
        }

        Job job = jobOptional.get();

        // Only return active jobs for public API
        if (job.getStatus() != Job.JobStatus.ACTIVE) {
            log.debug("Job {} is not active (status: {}), returning null", id, job.getStatus());
            return null;
        }

        // Convert entity to DTO - this accesses lazy relationships within the transaction
        // The DTO contains only primitive/simple types, safe for Redis caching
        PublicJobDto dto = PublicJobDto.fromEntity(job);
        log.debug("Converted job {} to PublicJobDto, will be cached", id);

        return dto;
    }

    /**
     * Check if a job exists and is active.
     * This method does NOT use cache - it's for validation purposes.
     *
     * @param id Job ID
     * @return true if job exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isJobActiveAndExists(Long id) {
        return jobRepository.findById(id)
                .map(job -> job.getStatus() == Job.JobStatus.ACTIVE)
                .orElse(false);
    }

    /**
     * Increment view count for a job.
     * This is done separately from the cached DTO fetch.
     * The view count is not part of the cached DTO (since it changes frequently).
     *
     * @param id Job ID
     */
    @Transactional
    public void incrementViews(Long id) {
        jobRepository.findById(id).ifPresent(job -> {
            job.setViewsCount(job.getViewsCount() + 1);
            jobRepository.save(job);
            log.debug("Incremented view count for job {}", id);
        });
    }

    /**
     * Evict public job cache entry.
     * Call this when a job is updated or deleted.
     *
     * @param id Job ID
     */
    @CacheEvict(value = CacheNames.PUBLIC_JOBS_BY_ID, key = "#id")
    public void evictPublicJobCache(Long id) {
        log.debug("Evicted public job cache for id: {}", id);
    }
}