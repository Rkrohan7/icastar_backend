package com.icastar.platform.service;

import com.icastar.platform.config.CacheNames;
import com.icastar.platform.constants.ApplicationConstants;

import com.icastar.platform.dto.job.CreateJobDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import com.icastar.platform.dto.job.JobDto;
import com.icastar.platform.dto.job.JobFilterDto;
import com.icastar.platform.dto.job.UpdateJobDto;
import com.icastar.platform.dto.job.*;
import com.icastar.platform.entity.Job;
import com.icastar.platform.entity.User;
import com.icastar.platform.repository.JobRepository;
import com.icastar.platform.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.JOBS_BY_ID, key = "#id")
    public Optional<Job> findById(Long id) {
        log.debug("Cache MISS: Loading job by id: {}", id);
        return jobRepository.findById(id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.JOBS_ACTIVE, key = "'all-active'")
    public List<Job> findActiveJobs() {
        log.debug("Cache MISS: Loading all active jobs");
        return jobRepository.findActiveJobs(LocalDate.now());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.JOBS_ACTIVE, key = "'page-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Job> findActiveJobs(Pageable pageable) {
        log.debug("Cache MISS: Loading active jobs page {}", pageable.getPageNumber());
        return jobRepository.findActiveJobs(LocalDate.now(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Job> findJobsWithFilters(JobFilterDto filter, Pageable pageable) {
        Page<Job> jobs = jobRepository.findJobsWithFilters(
                filter.getSearchTerm(),
                filter.getJobType(),
                filter.getExperienceLevel(),
                filter.getStatus(),
                filter.getMinPay(),
                filter.getMaxPay(),
                filter.getLocation(),
                filter.getIsRemote(),
                filter.getIsUrgent(),
                filter.getIsFeatured(),
                pageable
        );
        
        // Apply skills filter if provided
        if (filter.getSkills() != null && !filter.getSkills().isEmpty()) {
            List<Job> filteredJobs = jobs.getContent().stream()
                    .filter(job -> {
                        if (job.getSkillsRequired() == null) return false;
                        return filter.getSkills().stream()
                                .anyMatch(skill -> job.getSkillsRequired().toLowerCase().contains(skill.toLowerCase()));
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            // Create a new Page with filtered content
            return new org.springframework.data.domain.PageImpl<>(
                    filteredJobs, 
                    pageable, 
                    filteredJobs.size()
            );
        }
        
        return jobs;
    }

    @Transactional(readOnly = true)
    public List<Job> findByRecruiter(User recruiter) {
        return jobRepository.findByRecruiter(recruiter);
    }

    @Transactional(readOnly = true)
    public Page<Job> findByRecruiter(User recruiter, Pageable pageable) {
        return jobRepository.findByRecruiter(recruiter, pageable);
    }

    public Page<Job> findByRecruiter(Long recruiterId, Pageable pageable) {
        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
        return jobRepository.findByRecruiter(recruiter, pageable);
    }

    @Transactional(readOnly = true)
    public List<Job> searchJobs(String searchTerm) {
        return jobRepository.findBySearchTerm(searchTerm);
    }

    @Transactional(readOnly = true)
    public Page<Job> searchJobs(String searchTerm, Pageable pageable) {
        return jobRepository.findBySearchTerm(searchTerm, pageable);
    }

    @Transactional(readOnly = true)
    public List<Job> findJobsByLocation(String location) {
        return jobRepository.findByLocationContainingIgnoreCase(location);
    }

    @Transactional(readOnly = true)
    public List<Job> findJobsByType(Job.JobType jobType) {
        return jobRepository.findByJobType(jobType);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.JOBS_ACTIVE, key = "'remote'")
    public List<Job> findRemoteJobs() {
        log.debug("Cache MISS: Loading remote jobs");
        return jobRepository.findByIsRemoteTrue();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.JOBS_FEATURED, key = "'all-featured'")
    public List<Job> findFeaturedJobs() {
        log.debug("Cache MISS: Loading featured jobs");
        return jobRepository.findByIsFeaturedTrue();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.JOBS_ACTIVE, key = "'urgent'")
    public List<Job> findUrgentJobs() {
        log.debug("Cache MISS: Loading urgent jobs");
        return jobRepository.findByIsUrgentTrue();
    }

    @Transactional(readOnly = true)
    public List<Job> findJobsBySkill(String skill) {
        return jobRepository.findByRequiredSkill(skill);
    }

    @Transactional(readOnly = true)
    public List<Job> findJobsByTag(String tag) {
        return jobRepository.findByTag(tag);
    }

    @Transactional(readOnly = true)
    public List<Job> findMostPopularJobs(Pageable pageable) {
        return jobRepository.findMostPopularJobs(pageable);
    }

    @Transactional(readOnly = true)
    public List<Job> findRecentlyPostedJobs(Pageable pageable) {
        return jobRepository.findRecentlyPostedJobs(pageable);
    }

    @Transactional(readOnly = true)
    public List<Job> findJobsExpiringSoon(int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        return jobRepository.findJobsExpiringSoon(startDate, endDate);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.JOBS_ACTIVE, allEntries = true),
        @CacheEvict(value = CacheNames.JOBS_FEATURED, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    })
    public Job createJob(Long recruiterId, CreateJobDto createJobDto) {
        log.info("Creating new job - Cache will be evicted");
        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));

        Job job = new Job();
        job.setRecruiter(recruiter);
        job.setTitle(createJobDto.getTitle());
        job.setDescription(createJobDto.getDescription());
        job.setRequirements(createJobDto.getRequirements());
        job.setLocation(createJobDto.getLocation());
        job.setJobType(createJobDto.getJobType());
        job.setExperienceLevel(createJobDto.getExperienceLevel());
        job.setBudgetMin(createJobDto.getBudgetMin());
        job.setBudgetMax(createJobDto.getBudgetMax());
        job.setCurrency(createJobDto.getCurrency());
        job.setDurationDays(createJobDto.getDurationDays());
        job.setStartDate(createJobDto.getStartDate());
        job.setEndDate(createJobDto.getEndDate());
        job.setApplicationDeadline(createJobDto.getApplicationDeadline());
        job.setIsRemote(createJobDto.getIsRemote());
        job.setIsUrgent(createJobDto.getIsUrgent());
        job.setIsFeatured(createJobDto.getIsFeatured());
        job.setContactEmail(createJobDto.getContactEmail());
        job.setContactPhone(createJobDto.getContactPhone());
        job.setBenefits(createJobDto.getBenefits());

        // Convert lists to JSON strings
        try {
            if (createJobDto.getTags() != null) {
                job.setTags(objectMapper.writeValueAsString(createJobDto.getTags()));
            }
            if (createJobDto.getSkillsRequired() != null) {
                job.setSkillsRequired(objectMapper.writeValueAsString(createJobDto.getSkillsRequired()));
            }
        } catch (JsonProcessingException e) {
            log.error("Error converting lists to JSON", e);
            throw new RuntimeException("Error processing job data");
        }

        job.setStatus(Job.JobStatus.ACTIVE);
        job.setPublishedAt(LocalDateTime.now());

        return jobRepository.save(job);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.JOBS_BY_ID, key = "#jobId"),
        @CacheEvict(value = CacheNames.JOBS_ACTIVE, allEntries = true),
        @CacheEvict(value = CacheNames.JOBS_FEATURED, allEntries = true)
    })
    public Job updateJob(Long jobId, Long recruiterId, UpdateJobDto updateJobDto) {
        log.info("Updating job {} - Cache will be evicted", jobId);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.JOB_NOT_FOUND));

        // Verify ownership - only the recruiter who created the job can update it
        if (!job.getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("You don't have permission to update this job");
        }

        // Track if any field was actually updated
        boolean isUpdated = false;

        // Only update fields that are not null and different from current value
        if (updateJobDto.getTitle() != null && !updateJobDto.getTitle().equals(job.getTitle())) {
            job.setTitle(updateJobDto.getTitle());
            isUpdated = true;
        }
        if (updateJobDto.getDescription() != null && !updateJobDto.getDescription().equals(job.getDescription())) {
            job.setDescription(updateJobDto.getDescription());
            isUpdated = true;
        }
        if (updateJobDto.getRequirements() != null && !updateJobDto.getRequirements().equals(job.getRequirements())) {
            job.setRequirements(updateJobDto.getRequirements());
            isUpdated = true;
        }
        if (updateJobDto.getLocation() != null && !updateJobDto.getLocation().equals(job.getLocation())) {
            job.setLocation(updateJobDto.getLocation());
            isUpdated = true;
        }
        if (updateJobDto.getJobType() != null && !updateJobDto.getJobType().equals(job.getJobType())) {
            job.setJobType(updateJobDto.getJobType());
            isUpdated = true;
        }
        if (updateJobDto.getExperienceLevel() != null && !updateJobDto.getExperienceLevel().equals(job.getExperienceLevel())) {
            job.setExperienceLevel(updateJobDto.getExperienceLevel());
            isUpdated = true;
        }
        if (updateJobDto.getBudgetMin() != null && !updateJobDto.getBudgetMin().equals(job.getBudgetMin())) {
            job.setBudgetMin(updateJobDto.getBudgetMin());
            isUpdated = true;
        }
        if (updateJobDto.getBudgetMax() != null && !updateJobDto.getBudgetMax().equals(job.getBudgetMax())) {
            job.setBudgetMax(updateJobDto.getBudgetMax());
            isUpdated = true;
        }
        if (updateJobDto.getCurrency() != null && !updateJobDto.getCurrency().equals(job.getCurrency())) {
            job.setCurrency(updateJobDto.getCurrency());
            isUpdated = true;
        }
        if (updateJobDto.getDurationDays() != null && !updateJobDto.getDurationDays().equals(job.getDurationDays())) {
            job.setDurationDays(updateJobDto.getDurationDays());
            isUpdated = true;
        }
        if (updateJobDto.getStartDate() != null && !updateJobDto.getStartDate().equals(job.getStartDate())) {
            job.setStartDate(updateJobDto.getStartDate());
            isUpdated = true;
        }
        if (updateJobDto.getEndDate() != null && !updateJobDto.getEndDate().equals(job.getEndDate())) {
            job.setEndDate(updateJobDto.getEndDate());
            isUpdated = true;
        }
        if (updateJobDto.getApplicationDeadline() != null && !updateJobDto.getApplicationDeadline().equals(job.getApplicationDeadline())) {
            job.setApplicationDeadline(updateJobDto.getApplicationDeadline());
            isUpdated = true;
        }
        if (updateJobDto.getIsRemote() != null && !updateJobDto.getIsRemote().equals(job.getIsRemote())) {
            job.setIsRemote(updateJobDto.getIsRemote());
            isUpdated = true;
        }
        if (updateJobDto.getIsUrgent() != null && !updateJobDto.getIsUrgent().equals(job.getIsUrgent())) {
            job.setIsUrgent(updateJobDto.getIsUrgent());
            isUpdated = true;
        }
        if (updateJobDto.getIsFeatured() != null && !updateJobDto.getIsFeatured().equals(job.getIsFeatured())) {
            job.setIsFeatured(updateJobDto.getIsFeatured());
            isUpdated = true;
        }
        if (updateJobDto.getContactEmail() != null && !updateJobDto.getContactEmail().equals(job.getContactEmail())) {
            job.setContactEmail(updateJobDto.getContactEmail());
            isUpdated = true;
        }
        if (updateJobDto.getContactPhone() != null && !updateJobDto.getContactPhone().equals(job.getContactPhone())) {
            job.setContactPhone(updateJobDto.getContactPhone());
            isUpdated = true;
        }
        if (updateJobDto.getBenefits() != null && !updateJobDto.getBenefits().equals(job.getBenefits())) {
            job.setBenefits(updateJobDto.getBenefits());
            isUpdated = true;
        }
        if (updateJobDto.getStatus() != null && !updateJobDto.getStatus().equals(job.getStatus())) {
            job.setStatus(updateJobDto.getStatus());
            isUpdated = true;
        }

        // Update JSON fields if provided
        if (updateJobDto.getTags() != null && !updateJobDto.getTags().equals(job.getTags())) {
            job.setTags(updateJobDto.getTags());
            isUpdated = true;
        }
        if (updateJobDto.getSkillsRequired() != null && !updateJobDto.getSkillsRequired().equals(job.getSkillsRequired())) {
            job.setSkillsRequired(updateJobDto.getSkillsRequired());
            isUpdated = true;
        }

        // Only update audit field if record was actually modified
        if (isUpdated) {
            job.setUpdatedAt(LocalDateTime.now());
        }

        return jobRepository.save(job);
    }

    /**
     * Admin-only method to update any job without ownership validation
     */
    @Caching(evict = {
        @CacheEvict(value = CacheNames.JOBS_BY_ID, key = "#jobId"),
        @CacheEvict(value = CacheNames.JOBS_ACTIVE, allEntries = true),
        @CacheEvict(value = CacheNames.JOBS_FEATURED, allEntries = true)
    })
    public Job updateJobAsAdmin(Long jobId, UpdateJobDto updateJobDto) {
        log.info("Admin updating job {} - Cache will be evicted", jobId);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.JOB_NOT_FOUND));

        // Admin can update any job - no ownership check
        if (updateJobDto.getTitle() != null) {
            job.setTitle(updateJobDto.getTitle());
        }
        if (updateJobDto.getDescription() != null) {
            job.setDescription(updateJobDto.getDescription());
        }
        if (updateJobDto.getRequirements() != null) {
            job.setRequirements(updateJobDto.getRequirements());
        }
        if (updateJobDto.getLocation() != null) {
            job.setLocation(updateJobDto.getLocation());
        }
        if (updateJobDto.getJobType() != null) {
            job.setJobType(updateJobDto.getJobType());
        }
        if (updateJobDto.getExperienceLevel() != null) {
            job.setExperienceLevel(updateJobDto.getExperienceLevel());
        }
        if (updateJobDto.getBudgetMin() != null) {
            job.setBudgetMin(updateJobDto.getBudgetMin());
        }
        if (updateJobDto.getBudgetMax() != null) {
            job.setBudgetMax(updateJobDto.getBudgetMax());
        }
        if (updateJobDto.getCurrency() != null) {
            job.setCurrency(updateJobDto.getCurrency());
        }
        if (updateJobDto.getDurationDays() != null) {
            job.setDurationDays(updateJobDto.getDurationDays());
        }
        if (updateJobDto.getStartDate() != null) {
            job.setStartDate(updateJobDto.getStartDate());
        }
        if (updateJobDto.getEndDate() != null) {
            job.setEndDate(updateJobDto.getEndDate());
        }
        if (updateJobDto.getApplicationDeadline() != null) {
            job.setApplicationDeadline(updateJobDto.getApplicationDeadline());
        }
        if (updateJobDto.getIsRemote() != null) {
            job.setIsRemote(updateJobDto.getIsRemote());
        }
        if (updateJobDto.getIsUrgent() != null) {
            job.setIsUrgent(updateJobDto.getIsUrgent());
        }
        if (updateJobDto.getIsFeatured() != null) {
            job.setIsFeatured(updateJobDto.getIsFeatured());
        }
        if (updateJobDto.getContactEmail() != null) {
            job.setContactEmail(updateJobDto.getContactEmail());
        }
        if (updateJobDto.getContactPhone() != null) {
            job.setContactPhone(updateJobDto.getContactPhone());
        }
        if (updateJobDto.getBenefits() != null) {
            job.setBenefits(updateJobDto.getBenefits());
        }
        if (updateJobDto.getStatus() != null) {
            job.setStatus(updateJobDto.getStatus());
        }
        if (updateJobDto.getTags() != null) {
            job.setTags(updateJobDto.getTags());
        }
        if (updateJobDto.getSkillsRequired() != null) {
            job.setSkillsRequired(updateJobDto.getSkillsRequired());
        }

        return jobRepository.save(job);
    }

    /**
     * Admin-only method to delete any job without ownership validation
     */
    @Caching(evict = {
        @CacheEvict(value = CacheNames.JOBS_BY_ID, key = "#jobId"),
        @CacheEvict(value = CacheNames.JOBS_ACTIVE, allEntries = true),
        @CacheEvict(value = CacheNames.JOBS_FEATURED, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    })
    public void deleteJobAsAdmin(Long jobId) {
        log.info("Admin deleting job {} - Cache will be evicted", jobId);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.JOB_NOT_FOUND));

        job.setStatus(Job.JobStatus.CANCELLED);
        job.setClosedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.JOBS_BY_ID, key = "#jobId"),
        @CacheEvict(value = CacheNames.JOBS_ACTIVE, allEntries = true),
        @CacheEvict(value = CacheNames.JOBS_FEATURED, allEntries = true)
    })
    public Job updateJobStatus(Long jobId, Job.JobStatus status) {
        log.info("Updating job {} status to {} - Cache will be evicted", jobId, status);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.JOB_NOT_FOUND));

        job.setStatus(status);
        if (status == Job.JobStatus.CLOSED) {
            job.setClosedAt(LocalDateTime.now());
        }

        return jobRepository.save(job);
    }

    /**
     * Update job status with recruiter ownership validation and audit logging
     * @param jobId Job ID
     * @param recruiterId Recruiter ID (for ownership validation)
     * @param status New status
     * @param reason Optional reason for status change
     * @return Updated job
     */
    @Caching(evict = {
        @CacheEvict(value = CacheNames.JOBS_BY_ID, key = "#jobId"),
        @CacheEvict(value = CacheNames.JOBS_ACTIVE, allEntries = true),
        @CacheEvict(value = CacheNames.JOBS_FEATURED, allEntries = true)
    })
    public Job updateJobStatus(Long jobId, Long recruiterId, Job.JobStatus status, String reason) {
        log.info("Attempting to update job {} status to {} by recruiter {} - Cache will be evicted", jobId, status, recruiterId);

        // Validate inputs
        if (jobId == null) {
            throw new RuntimeException("Job ID cannot be null");
        }
        if (recruiterId == null) {
            throw new RuntimeException("Recruiter ID cannot be null");
        }
        if (status == null) {
            throw new RuntimeException("Status cannot be null");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.JOB_NOT_FOUND));

        // Verify recruiter exists and is not null
        if (job.getRecruiter() == null) {
            log.error("Job {} has no recruiter assigned", jobId);
            throw new RuntimeException("Job has no recruiter assigned");
        }

        // Verify ownership
        if (!job.getRecruiter().getId().equals(recruiterId)) {
            log.warn("Recruiter {} attempted to update status of job {} owned by recruiter {}",
                    recruiterId, jobId, job.getRecruiter().getId());
            throw new RuntimeException("You don't have permission to update this job");
        }

        Job.JobStatus oldStatus = job.getStatus();
        log.info("Current job status: {}, requested status: {}", oldStatus, status);

        // Validate status transition
        validateStatusTransition(oldStatus, status);

        // Update status
        job.setStatus(status);

        // Set timestamps based on status
        if (status == Job.JobStatus.ACTIVE && oldStatus == Job.JobStatus.DRAFT) {
            job.setPublishedAt(LocalDateTime.now());
            log.info("Setting publishedAt timestamp for job {}", jobId);
        }

        if (status == Job.JobStatus.CLOSED) {
            job.setClosedAt(LocalDateTime.now());
            log.info("Setting closedAt timestamp for job {}", jobId);
        }

        Job updatedJob = jobRepository.save(job);

        // Log status change
        log.info("Job {} status changed from {} to {} by recruiter {}. Reason: {}",
                jobId, oldStatus, status, recruiterId, reason != null ? reason : "Not specified");

        return updatedJob;
    }

    /**
     * Validate job status transition
     */
    private void validateStatusTransition(Job.JobStatus from, Job.JobStatus to) {
        if (from == null || to == null) {
            throw new RuntimeException("Status cannot be null");
        }

        boolean isValidTransition = false;

        switch (from) {
            case DRAFT:
                isValidTransition = (to == Job.JobStatus.ACTIVE || to == Job.JobStatus.CLOSED);
                break;
            case ACTIVE:
                isValidTransition = (to == Job.JobStatus.PAUSED || to == Job.JobStatus.CLOSED || to == Job.JobStatus.CANCELLED || to == Job.JobStatus.DRAFT);
                break;
            case PAUSED:
                isValidTransition = (to == Job.JobStatus.ACTIVE || to == Job.JobStatus.CLOSED || to == Job.JobStatus.CANCELLED);
                break;
            case CLOSED:
                isValidTransition = (to == Job.JobStatus.ACTIVE || to == Job.JobStatus.DRAFT); // Allow reopening to ACTIVE or DRAFT
                break;
            case CANCELLED:
                isValidTransition = false; // Cannot transition from cancelled
                break;
            case DELETED:
                isValidTransition = false; // Cannot transition from deleted
                break;
            default:
                isValidTransition = false;
                break;
        }

        if (!isValidTransition) {
            throw new RuntimeException(String.format("Invalid status transition from %s to %s", from, to));
        }
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.JOBS_BY_ID, key = "#jobId"),
        @CacheEvict(value = CacheNames.JOBS_ACTIVE, allEntries = true),
        @CacheEvict(value = CacheNames.JOBS_FEATURED, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    })
    public void deleteJob(Long jobId, Long recruiterId) {
        log.info("Deleting job {} by recruiter {} - Cache will be evicted", jobId, recruiterId);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.JOB_NOT_FOUND));

        // Verify ownership - only the recruiter who created the job can delete it
        if (!job.getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("You don't have permission to delete this job");
        }
        jobRepository.deleteById(jobId);
    }

    public void incrementViews(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.JOB_NOT_FOUND));

        job.setViewsCount(job.getViewsCount() + 1);
        jobRepository.save(job);
    }

    public void incrementApplications(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.JOB_NOT_FOUND));

        job.setApplicationsCount(job.getApplicationsCount() + 1);
        jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Long countJobsByRecruiter(User recruiter) {
        return jobRepository.countByRecruiter(recruiter);
    }

    @Transactional(readOnly = true)
    public Long countJobsByStatus(Job.JobStatus status) {
        return jobRepository.countByStatus(status);
    }

    public Job toggleJobVisibility(Long jobId, Long recruiterId) {
        Job job = findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));

        // Verify ownership - only the recruiter who created the job can toggle visibility
        if (!job.getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("You don't have permission to modify this job");
        }

        job.setStatus(job.getStatus() == Job.JobStatus.ACTIVE ? Job.JobStatus.CLOSED : Job.JobStatus.ACTIVE);
        return jobRepository.save(job);
    }

    public Long getJobsCountByRecruiter(Long recruiterId) {
        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
        return countJobsByRecruiter(recruiter);
    }

    public List<Job> getFeaturedJobs(int limit) {
        return jobRepository.findByIsFeaturedTrue()
                .stream()
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Job> getRecentJobs(int limit) {
        return jobRepository.findAll()
                .stream()
                .sorted((j1, j2) -> j2.getPublishedAt().compareTo(j1.getPublishedAt()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    public Long getTotalJobsCount() {
        return jobRepository.count();
    }

    public Long getActiveJobsCount() {
        return countJobsByStatus(Job.JobStatus.ACTIVE);
    }


    public BulkUploadResult bulkUploadJobs(Long recruiterId, MultipartFile file) {
        String filename = file.getOriginalFilename().toLowerCase();
        List<CreateJobDto> jobDtos = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {
            if (filename.endsWith(".csv")) {
                jobDtos = parseCsv(inputStream);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                jobDtos = parseExcel(inputStream);
            } else {
                throw new RuntimeException("Unsupported file format. Only CSV or XLSX allowed.");
            }

            int success = 0;
            int failed = 0;
            List<String> errorMessages = new ArrayList<>();

            for (int i = 0; i < jobDtos.size(); i++) {
                CreateJobDto dto = jobDtos.get(i);
                try {
                    createJob(recruiterId, dto);
                    success++;
                } catch (Exception ex) {
                    failed++;
                    errorMessages.add("Row " + (i + 1) + ": " + ex.getMessage());
                    log.error("Error saving job at row {}: {}", i + 1, ex.getMessage());
                }
            }

            return new BulkUploadResult(success, failed, errorMessages);

        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + e.getMessage());
        }
    }

    private List<CreateJobDto> parseCsv(InputStream inputStream) throws IOException {
        List<CreateJobDto> jobs = new ArrayList<>();

        try (Reader reader = new InputStreamReader(inputStream);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext(); // skip header

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                CreateJobDto dto = new CreateJobDto();
                dto.setTitle(line[0]);
                dto.setDescription(line[1]);
                dto.setLocation(line[2]);
                dto.setBudgetMin(new BigDecimal(line[3]));
                dto.setBudgetMax(new BigDecimal(line[4]));
                dto.setJobType(Job.JobType.valueOf(line[5].toUpperCase()));
                dto.setExperienceLevel(Job.ExperienceLevel.valueOf(line[6].toUpperCase()));
                dto.setIsRemote(Boolean.parseBoolean(line[7]));
                dto.setSkillsRequired(String.join(",", line[8].split(",")));

                jobs.add(dto);
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }

        return jobs;
    }

    private List<CreateJobDto> parseExcel(InputStream inputStream) throws IOException {
        List<CreateJobDto> jobs = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        Iterator<Row> iterator = sheet.iterator();
        if (iterator.hasNext()) iterator.next(); // skip header row

        while (iterator.hasNext()) {
            Row row = iterator.next();
            CreateJobDto dto = new CreateJobDto();

            dto.setTitle(getString(row, 0));
            dto.setDescription(getString(row, 1));
            dto.setLocation(getString(row, 2));
            dto.setBudgetMin(getBigDecimal(row, 3));
            dto.setBudgetMax(getBigDecimal(row, 4));
            dto.setJobType(Job.JobType.valueOf(getString(row, 5).toUpperCase()));
            dto.setExperienceLevel(Job.ExperienceLevel.valueOf(getString(row, 6).toUpperCase()));
            dto.setIsRemote(Boolean.parseBoolean(getString(row, 7)));
            dto.setSkillsRequired(getString(row, 8));

            jobs.add(dto);
        }

        workbook.close();
        return jobs;
    }

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col);
        return cell != null ? cell.toString().trim() : "";
    }

    private BigDecimal getBigDecimal(Row row, int col) {
        try {
            return new BigDecimal(getString(row, col));
        } catch (Exception e) {
            return null;
        }
    }

}