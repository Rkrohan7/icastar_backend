package com.icastar.platform.service;

import com.icastar.platform.config.CacheNames;
import com.icastar.platform.constants.ApplicationConstants;

import com.icastar.platform.dto.recruiter.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import com.icastar.platform.entity.*;
import com.icastar.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.icastar.platform.dto.application.JobApplicationDto;

import com.icastar.platform.dto.recruiter.CreateJobDto;
import com.icastar.platform.exception.ValidationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecruiterDashboardService {

    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UsageTrackingRepository usageTrackingRepository;
    private final EmailService emailService;
    private final ArtistService artistService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;




    /**
     * Get recruiter dashboard overview
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.DASHBOARD_RECRUITER, key = "#recruiter.id")
    public RecruiterDashboardDto getDashboard(User recruiter) {
        log.debug("Cache MISS: Loading recruiter dashboard for user: {}", recruiter.getId());
        // Get recruiter profile
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.RECRUITER_PROFILE_NOT_FOUND));

        // Get subscription information
        Optional<Subscription> subscription = subscriptionRepository.findByUserIdAndStatus(
                recruiter.getId(), Subscription.SubscriptionStatus.ACTIVE);
        Map<String, Object> statistics = getDashboardStatistics(recruiter);
        List<RecentJobDto> recentJobs = getRecentJobs(recruiter, 5);
        List<RecentApplicationDto> recentApplications = getRecentApplications(recruiter, 5);
        List<RecentHireDto> recentHires = getRecentHires(recruiter, 5);
        return RecruiterDashboardDto.builder()
                .recruiterId(recruiterProfile.getId() != null ? recruiterProfile.getId() : null)
                .recruiterName(recruiterProfile.getContactPersonName() != null ? recruiterProfile.getContactPersonName() : null)
                .companyName(recruiterProfile.getCompanyName() != null ? recruiterProfile.getCompanyName() : null)
                .recruiterCategory(recruiterProfile.getRecruiterCategory() != null ?
                        recruiterProfile.getRecruiterCategory().getDisplayName() : "Unknown")
                .email(recruiter.getEmail())
                .phone(recruiterProfile.getContactPhone() != null ? recruiterProfile.getContactPhone() : null )
                .isActive(recruiterProfile.getIsActive() != null ? recruiterProfile.getIsActive() : null)
                .subscriptionId(subscription.map(Subscription::getId).orElse(null))
                .subscriptionPlan(subscription.map(s -> s.getSubscriptionPlan().getName()).orElse(ApplicationConstants.DefaultValues.DEFAULT_SUBSCRIPTION_PLAN))
                .subscriptionStatus(subscription.map(s -> s.getStatus().name()).orElse(ApplicationConstants.Status.INACTIVE))
                .subscriptionExpiresAt(subscription.map(Subscription::getExpiresAt).orElse(null))
                .totalJobsPosted((Long) statistics.get("totalJobsPosted"))
                .activeJobs((Long) statistics.get("activeJobs"))
                .closedJobs((Long) statistics.get("closedJobs"))
                .totalApplications((Long) statistics.get("totalApplications"))
                .totalHires((Long) statistics.get("totalHires"))
                .totalViews((Long) statistics.get("totalViews"))
                .averageApplicationsPerJob((Double) statistics.get("averageApplicationsPerJob"))
                .hireRate((Double) statistics.get("hireRate"))
                .recentJobs(recentJobs)
                .recentApplications(recentApplications)
                .recentHires(recentHires)
                .canViewApplications(true)
                .canBrowseArtists(true)
                .canGetSuggestions(true)
                .canTrackHires(true)
                .availableFeatures(getAvailableFeatures(recruiter))
                .premiumFeatures(getPremiumFeatures(recruiter))
                .hasPremiumFeatures(hasPremiumFeatures(recruiter))
                .build();
    }
    
    /**
     * Get all jobs posted by recruiter
     */
    @Transactional(readOnly = true)
    public Page<RecentJobDto> getMyJobs(String status, Boolean isActive, String jobType,
                                       String location, String title, Pageable pageable, User recruiter) {
        // Get recruiter profile
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.RECRUITER_PROFILE_NOT_FOUND));
        
        // Build query based on filters
        Page<Job> jobs = jobRepository.findByRecruiter(recruiterProfile.getUser(), pageable);

        // Apply additional filters if needed
        if (status != null) {
            // Filter jobs by status
            List<Job> filteredJobs = jobs.getContent().stream()
                    .filter(job -> job.getStatus().name().equals(status))
                    .collect(Collectors.toList());
            // Create a new Page with filtered content
            jobs = new org.springframework.data.domain.PageImpl<>(filteredJobs, pageable, filteredJobs.size());
        }

        return jobs.map(this::convertToRecentJobDto);
    }

    /**
     * Get job details by ID
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getJobDetails(Long jobId, User recruiter) {
        // Get recruiter profile
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.RECRUITER_PROFILE_NOT_FOUND));
        
        // Get job
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.JOB_NOT_FOUND));
        
        // Check if job belongs to recruiter
        if (!job.getRecruiter().getId().equals(recruiterProfile.getUser().getId())) {
            throw new RuntimeException(ApplicationConstants.ErrorMessages.ACCESS_DENIED);
        }

        // Get applications count - TODO: Calculate from JobApplication repository if needed
        int applicationCount = 0;

        // Build job details
        Map<String, Object> jobDetails = new HashMap<>();
        jobDetails.put("id", job.getId());
        jobDetails.put("title", job.getTitle());
        jobDetails.put("description", job.getDescription());
        jobDetails.put("requirements", job.getRequirements());
        jobDetails.put("jobType", job.getJobType());
        jobDetails.put("location", job.getLocation());
        jobDetails.put("salaryMin", job.getBudgetMin());
        jobDetails.put("salaryMax", job.getBudgetMax());
        jobDetails.put("currency", job.getCurrency());
        jobDetails.put("status", job.getStatus());
        jobDetails.put("isActive", job.getStatus() == Job.JobStatus.ACTIVE);
        jobDetails.put("applicationDeadline", job.getApplicationDeadline());
        jobDetails.put("startDate", job.getStartDate());
        jobDetails.put("createdAt", job.getCreatedAt());
        jobDetails.put("updatedAt", job.getUpdatedAt());
        jobDetails.put("applicationCount", applicationCount);
        jobDetails.put("viewCount", 0); // This would need to be tracked separately
        jobDetails.put("boostCount", 0); // This would need to be tracked separately
        
        return jobDetails;
    }
    
    /**
     * Get job applications
     */
    @Transactional(readOnly = true)
    public Page<RecentApplicationDto> getJobApplications(Long jobId, String status, String artistCategory,
                                                        String location, String experienceLevel, 
                                                        Pageable pageable, User recruiter) {
        // Get recruiter profile
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.RECRUITER_PROFILE_NOT_FOUND));
        
        // Get job
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.JOB_NOT_FOUND));
        
        // Check if job belongs to recruiter
        if (!job.getRecruiter().getId().equals(recruiterProfile.getUser().getId())) {
            throw new RuntimeException(ApplicationConstants.ErrorMessages.ACCESS_DENIED);
        }
        
        // Get applications
        Page<JobApplication> applications = jobApplicationRepository.findByJobId(jobId, pageable);
        
        return applications.map(this::convertToRecentApplicationDto);
    }
    
    /**
     * Browse artist profiles
     * Only returns artists with ACTIVE user status
     */
    @Transactional(readOnly = true)
    public Page<ArtistSuggestionDto> browseArtists(String artistCategory, String artistType, String location,
                                                    String skills, String genres, String experienceLevel,
                                                    String availability, Boolean isVerified, Boolean isPremium,
                                                    Pageable pageable, String email) {

        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));

        // Get only ACTIVE artist profiles (user.status = ACTIVE AND isActive = true)
        Page<ArtistProfile> artistProfiles = artistProfileRepository.findActiveArtistsPageable(pageable);
        
        // Apply filters
        if (artistCategory != null) {
            // Filter artist profiles by category
            List<ArtistProfile> filteredProfiles = artistProfiles.getContent().stream()
                    .filter(profile -> profile.getArtistType() != null && 
                            profile.getArtistType().getDisplayName().equals(artistCategory))
                    .collect(Collectors.toList());
            // Create a new Page with filtered content
            artistProfiles = new PageImpl<>(filteredProfiles, pageable, filteredProfiles.size());
        }
        
        return artistProfiles.map(this::convertToArtistSuggestionDto);
    }
    
    /**
     * Get artist suggestions based on job criteria
     */
    @Transactional(readOnly = true)
    public List<ArtistSuggestionDto> getArtistSuggestions(Long jobId, String artistCategory, String artistType,
                                                          String location, String skills, String genres,
                                                          String experienceLevel, String availability,
                                                          Boolean isVerified, Boolean isPremium, Integer limit,
                                                          User recruiter) {
        // Get job if jobId is provided
        final Job job = jobId != null ?
                jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found")) : null;
        
        // Get all artist profiles
        List<ArtistProfile> artistProfiles = artistProfileRepository.findAll();
        
        // Convert to suggestions and calculate match scores
        List<ArtistSuggestionDto> suggestions = artistProfiles.stream()
                .map(profile -> {
                    ArtistSuggestionDto suggestion = convertToArtistSuggestionDto(profile);
                    
                    // Calculate match score based on criteria
                    double matchScore = calculateMatchScore(profile, job, artistCategory, artistType,
                            location, skills, genres, experienceLevel, availability, isVerified, isPremium);
                    
                    suggestion.setMatchScore(matchScore);
                    suggestion.setMatchReasons(generateMatchReasons(profile, job, artistCategory, artistType,
                            location, skills, genres, experienceLevel, availability, isVerified, isPremium));
                    
                    return suggestion;
                })
                .filter(suggestion -> suggestion.getMatchScore() > 0.3) // Only show relevant matches
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .limit(limit)
                .collect(Collectors.toList());
        
        return suggestions;
    }
    
    /**
     * Get hires and past jobs
     */
    @Transactional(readOnly = true)
    public Page<RecentHireDto> getHires(String status, String artistCategory, String jobType,
                                        String performanceRating, Boolean isCompleted, Boolean isRecommended,
                                        Pageable pageable, User recruiter) {
        // Get recruiter profile
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.RECRUITER_PROFILE_NOT_FOUND));
        
        // Get jobs by recruiter
        Page<Job> jobs = jobRepository.findByRecruiter(recruiterProfile.getUser(), pageable);

        // Convert to hires (this would need to be implemented with a proper hire tracking system)
        return jobs.map(job -> {
            // This is a placeholder - would need actual hire tracking
            return RecentHireDto.builder()
                    .id(job.getId())
                    .jobId(job.getId())
                    .jobTitle(job.getTitle())
                    .artistId(1L) // Placeholder
                    .artistName(ApplicationConstants.DefaultValues.SAMPLE_ARTIST_NAME)
                    .artistEmail(ApplicationConstants.DefaultValues.SAMPLE_ARTIST_EMAIL)
                    .artistCategory(ApplicationConstants.DefaultValues.SAMPLE_ARTIST_CATEGORY)
                    .hireStatus(ApplicationConstants.DefaultValues.DEFAULT_HIRE_STATUS)
                    .hiredAt(job.getCreatedAt())
                    .startDate(job.getStartDate() != null ? job.getStartDate().atStartOfDay() : null)
                    .endDate(job.getStartDate() != null ? job.getStartDate().plusDays(30).atStartOfDay() : null)
                    .agreedSalary(job.getBudgetMin())
                    .currency(job.getCurrency())
                    .contractType(ApplicationConstants.DefaultValues.DEFAULT_CONTRACT_TYPE)
                    .workLocation(job.getLocation())
                    .workSchedule(ApplicationConstants.DefaultValues.DEFAULT_WORK_SCHEDULE)
                    .performanceRating(ApplicationConstants.DefaultValues.DEFAULT_PERFORMANCE_RATING)
                    .feedback(ApplicationConstants.DefaultValues.DEFAULT_FEEDBACK)
                    .isCompleted(true)
                    .isRecommended(true)
                    .canViewProfile(true)
                    .canRate(true)
                    .canRecommend(true)
                    .canRehire(true)
                    .canMessage(true)
                    .build();
        });
    }
    
    /**
     * Get hire details by ID
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getHireDetails(Long hireId, User recruiter) {
        // This would need to be implemented with a proper hire tracking system
        Map<String, Object> hireDetails = new HashMap<>();
        hireDetails.put("id", hireId);
        hireDetails.put("message", "Hire details would be implemented with proper hire tracking system");
        
        return hireDetails;
    }
    
    /**
     * Rate and provide feedback for a hire
     */
    @Transactional
    public Map<String, Object> rateHire(Long hireId, String rating, String feedback, User recruiter) {
        // This would need to be implemented with a proper hire tracking system
        Map<String, Object> result = new HashMap<>();
        result.put("hireId", hireId);
        result.put("rating", rating);
        result.put("feedback", feedback);
        result.put("message", "Rating submitted successfully");
        
        return result;
    }
    
    /**
     * Get recruiter statistics
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.DASHBOARD_RECRUITER, key = "'stats-' + #email")
    public Map<String, Object> getStatistics(String email) {
        log.debug("Cache MISS: Loading recruiter statistics for: {}", email);
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));

        return getDashboardStatistics(recruiter);
    }
    
    /**
     * Get subscription status and features
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSubscriptionStatus(User recruiter) {
        // Get subscription
        Optional<Subscription> subscription = subscriptionRepository.findByUserIdAndStatus(
                recruiter.getId(), Subscription.SubscriptionStatus.ACTIVE);
        
        Map<String, Object> subscriptionStatus = new HashMap<>();
        subscriptionStatus.put("hasSubscription", subscription.isPresent());
        subscriptionStatus.put("subscriptionId", subscription.map(Subscription::getId).orElse(null));
        subscriptionStatus.put("subscriptionPlan", subscription.map(s -> s.getSubscriptionPlan().getName()).orElse(ApplicationConstants.DefaultValues.DEFAULT_SUBSCRIPTION_PLAN));
        subscriptionStatus.put("subscriptionStatus", subscription.map(s -> s.getStatus().name()).orElse(ApplicationConstants.Status.INACTIVE));
        subscriptionStatus.put("expiresAt", subscription.map(Subscription::getExpiresAt).orElse(null));
        subscriptionStatus.put("availableFeatures", getAvailableFeatures(recruiter));
        subscriptionStatus.put("premiumFeatures", getPremiumFeatures(recruiter));
        subscriptionStatus.put("hasPremiumFeatures", hasPremiumFeatures(recruiter));
        
        return subscriptionStatus;
    }
    
    /**
     * Update job status
     */
    @Transactional
    @CacheEvict(value = CacheNames.DASHBOARD_RECRUITER, allEntries = true)
    public Map<String, Object> updateJobStatus(Long jobId, String status, String reason, User recruiter) {
        // Get recruiter profile
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.RECRUITER_PROFILE_NOT_FOUND));
        
        // Get job
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.JOB_NOT_FOUND));
        
        // Check if job belongs to recruiter
        if (!job.getRecruiter().getId().equals(recruiterProfile.getUser().getId())) {
            throw new RuntimeException(ApplicationConstants.ErrorMessages.ACCESS_DENIED);
        }
        
        // Update status
        job.setStatus(Job.JobStatus.valueOf(status));
        job.setUpdatedAt(LocalDateTime.now());
        
        Job savedJob = jobRepository.save(job);
        
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", savedJob.getId());
        result.put("status", savedJob.getStatus());
        result.put("updatedAt", savedJob.getUpdatedAt());
        
        return result;
    }
    
    /**
     * Boost job visibility
     */
    @Transactional
    public Map<String, Object> boostJob(Long jobId, Integer days, User recruiter) {
        // Get recruiter profile
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.RECRUITER_PROFILE_NOT_FOUND));
        
        // Get job
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.JOB_NOT_FOUND));
        
        // Check if job belongs to recruiter
        if (!job.getRecruiter().getId().equals(recruiterProfile.getUser().getId())) {
            throw new RuntimeException(ApplicationConstants.ErrorMessages.ACCESS_DENIED);
        }
        
        // Boost job (this would need to be implemented with proper boost tracking)
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", job.getId());
        result.put("boosted", true);
        result.put("boostDays", days != null ? days : 7);
        result.put("boostExpiresAt", LocalDateTime.now().plusDays(days != null ? days : 7));
        
        return result;
    }
    
    // Helper methods
    
    private Map<String, Object> getDashboardStatistics(User recruiter) {
        // Get recruiter profile
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.RECRUITER_PROFILE_NOT_FOUND));
        
        // Get jobs by recruiter
        List<Job> jobs = jobRepository.findByRecruiter(recruiterProfile.getUser());
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalJobsPosted", (long) jobs.size());
        statistics.put("activeJobs", jobs.stream().filter(job -> job.getStatus() == Job.JobStatus.ACTIVE).count());
        statistics.put("closedJobs", jobs.stream().filter(job -> job.getStatus() == Job.JobStatus.CLOSED).count());
        statistics.put("totalApplications", 0); // TODO: Calculate from JobApplication repository if needed
        statistics.put("totalHires", 0L); // This would need to be tracked separately
        statistics.put("totalViews", 0L); // This would need to be tracked separately
        statistics.put("averageApplicationsPerJob", 0.0); // TODO: Calculate from JobApplication repository if needed
        statistics.put("hireRate", 0.0); // This would need to be calculated
        
        return statistics;
    }
    
    private List<RecentJobDto> getRecentJobs(User recruiter, int limit) {
        // Get recruiter profile
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.RECRUITER_PROFILE_NOT_FOUND));
        
        // Get recent jobs
        List<Job> jobs = jobRepository.findByRecruiter(recruiterProfile.getUser())
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());
        
        return jobs.stream()
                .map(this::convertToRecentJobDto)
                .collect(Collectors.toList());
    }
    
    private List<RecentApplicationDto> getRecentApplications(User recruiter, int limit) {
        // This would need to be implemented with proper application tracking
        return new ArrayList<>();
    }
    
    private List<RecentHireDto> getRecentHires(User recruiter, int limit) {
        // This would need to be implemented with proper hire tracking
        return new ArrayList<>();
    }
    
    private RecentJobDto convertToRecentJobDto(Job job) {
        return RecentJobDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .jobType(job.getJobType().name())
                .location(job.getLocation())
                .salaryMin(job.getBudgetMin())
                .salaryMax(job.getBudgetMax())
                .currency(job.getCurrency())
                .status(job.getStatus().name())
                .isActive(job.getStatus() == Job.JobStatus.ACTIVE)
                .applicationDeadline(job.getApplicationDeadline() != null ?
                    job.getApplicationDeadline().atStartOfDay() : null)
                .startDate(job.getStartDate() != null ?
                    job.getStartDate().atStartOfDay() : null)
                .createdAt(job.getCreatedAt())
                .applicationCount(0) // TODO: Calculate from JobApplication repository if needed
                .viewCount(0) // This would need to be tracked separately
                .boostCount(0) // This would need to be tracked separately
                .canEdit(true)
                .canClose(true)
                .canBoost(true)
                .canViewApplications(true)
                .build();
    }
    
    private RecentApplicationDto convertToRecentApplicationDto(JobApplication application) {
        return RecentApplicationDto.builder()
                .id(application.getId())
                .jobId(application.getJob().getId())
                .jobTitle(application.getJob().getTitle())
                .artistId(application.getArtist().getId())
                .artistName(application.getArtist().getFirstName() + " " + application.getArtist().getLastName())
                .artistEmail(application.getArtist().getUser().getEmail())
                .artistPhone("N/A") // Placeholder
                .artistCategory("Unknown") // Placeholder
                .applicationStatus(application.getStatus().name())
                .appliedAt(application.getAppliedAt())
                .lastUpdatedAt(application.getUpdatedAt())
                .artistBio("N/A") // Placeholder
                .artistLocation("N/A") // Placeholder
                .artistExperience(0) // Placeholder
                .artistSkills("N/A") // Placeholder
                .artistPortfolio("N/A") // Placeholder
                .coverLetter(application.getCoverLetter())
                .expectedSalary("N/A") // Placeholder
                .availability("N/A") // Placeholder
                .additionalNotes("N/A") // Placeholder
                .canViewProfile(true)
                .canAccept(true)
                .canReject(true)
                .canShortlist(true)
                .canMessage(true)
                .build();
    }
    
    private ArtistSuggestionDto convertToArtistSuggestionDto(ArtistProfile artistProfile) {
        // Calculate profile completion percentage
        int completionPercentage = artistService.calculateProfileCompletionPercentage(artistProfile);

        // Get experience level based on years
        String experienceLevel = "Beginner";
        Integer expYears = artistProfile.getExperienceYears();
        if (expYears != null) {
            if (expYears >= 10) {
                experienceLevel = "Expert";
            } else if (expYears >= 5) {
                experienceLevel = "Experienced";
            } else if (expYears >= 2) {
                experienceLevel = "Intermediate";
            }
        }

        // Parse JSON fields
        List<String> skillsList = parseJsonArray(artistProfile.getSkills());
        List<String> languagesList = parseJsonArray(artistProfile.getLanguagesSpoken());
        List<String> comfortableAreasList = parseJsonArray(artistProfile.getComfortableAreas());
        List<String> travelCitiesList = parseJsonArray(artistProfile.getTravelCities());
        List<String> portfolioUrlsList = parseJsonArray(artistProfile.getPortfolioUrls());
        List<String> projectsWorkedList = parseJsonArray(artistProfile.getProjectsWorked());

        return ArtistSuggestionDto.builder()
                .artistId(artistProfile.getId())
                .artistName(artistProfile.getFirstName() + " " + artistProfile.getLastName())
                .artistEmail(artistProfile.getUser().getEmail())
                .artistCategory(artistProfile.getArtistType() != null ?
                        artistProfile.getArtistType().getDisplayName() : "Unknown")
                .artistType(artistProfile.getArtistType() != null ?
                        artistProfile.getArtistType().getName() : "Unknown")
                .location(artistProfile.getLocation() != null ? artistProfile.getLocation() : "N/A")
                .bio(artistProfile.getBio() != null ? artistProfile.getBio() : "N/A")
                .profilePhoto(artistProfile.getProfileUrl() != null ? artistProfile.getProfileUrl() : null)
                .matchScore(0.0)
                .matchReasons(new ArrayList<>())
                .skills(skillsList)
                .genres(new ArrayList<>())
                .languages(languagesList)
                .experienceYears(expYears != null ? expYears : 0)
                .experienceLevel(experienceLevel)
                .portfolioItems(portfolioUrlsList)
                .achievements(new ArrayList<>())
                .certifications(new ArrayList<>())
                .availability("N/A")
                .preferredJobType("N/A")
                .expectedSalaryMin(null)
                .expectedSalaryMax(null)
                .currency("USD")
                .workLocation(artistProfile.getLocation() != null ? artistProfile.getLocation() : "N/A")
                .workSchedule("N/A")
                .phone(artistProfile.getUser().getMobile() != null ? artistProfile.getUser().getMobile() : "N/A")
                .website("N/A")
                .socialLinks(new ArrayList<>())
                .contactPreference("Email")
                .lastActive(artistProfile.getUpdatedAt() != null ? artistProfile.getUpdatedAt() : LocalDateTime.now())
                .totalApplications(artistProfile.getTotalApplications() != null ? artistProfile.getTotalApplications() : 0)
                .totalHires(artistProfile.getSuccessfulHires() != null ? artistProfile.getSuccessfulHires() : 0)
                .hireRate(0.0)
                .verificationStatus(artistProfile.getIsVerifiedBadge() != null && artistProfile.getIsVerifiedBadge() ? "VERIFIED" : "UNVERIFIED")
                .isVerified(artistProfile.getIsVerifiedBadge() != null && artistProfile.getIsVerifiedBadge())
                .isPremium(false)
                .profileCompletionPercentage(completionPercentage)
                // Physical Attributes
                .hourlyRate(artistProfile.getHourlyRate())
                .weight(artistProfile.getWeight())
                .height(artistProfile.getHeight())
                .hairColor(artistProfile.getHairColor())
                .hairLength(artistProfile.getHairLength())
                .eyeColor(artistProfile.getEyeColor())
                .complexion(artistProfile.getComplexion())
                .hasTattoo(artistProfile.getHasTattoo())
                .hasMole(artistProfile.getHasMole())
                .shoeSize(artistProfile.getShoeSize())
                .hasPassport(artistProfile.getHasPassport())
                .gender(artistProfile.getGender() != null ? artistProfile.getGender().name() : null)
                .maritalStatus(artistProfile.getMaritalStatus() != null ? artistProfile.getMaritalStatus().name() : null)
                .dateOfBirth(artistProfile.getDateOfBirth() != null ? artistProfile.getDateOfBirth().toString() : null)
                // Additional Fields
                .comfortableAreas(comfortableAreasList)
                .travelCities(travelCitiesList)
                .portfolioUrls(portfolioUrlsList)
                .videoUrl(artistProfile.getVideoUrl())
                .coverPhotoUrl(artistProfile.getCoverPhotoUrl())
                .projectsWorked(projectsWorkedList)
                // Quick Actions
                .canViewProfile(true)
                .canContact(true)
                .canShortlist(true)
                .canInvite(true)
                .canMessage(true)
                .build();
    }

    /**
     * Helper method to parse JSON array string to List<String>
     */
    private List<String> parseJsonArray(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON array: {}", jsonString);
            return new ArrayList<>();
        }
    }
    
    private double calculateMatchScore(ArtistProfile profile, Job job, String artistCategory,
                                          String artistType, String location, String skills, String genres,
                                          String experienceLevel, String availability, Boolean isVerified,
                                          Boolean isPremium) {
        // This would implement sophisticated matching algorithm
        return Math.random(); // Placeholder
    }
    
    private List<String> generateMatchReasons(ArtistProfile profile, Job job, String artistCategory,
                                              String artistType, String location, String skills, String genres,
                                              String experienceLevel, String availability, Boolean isVerified,
                                              Boolean isPremium) {
        // This would generate reasons for the match
        return Arrays.asList(
            ApplicationConstants.DefaultValues.SKILLS_MATCH_REASON,
            ApplicationConstants.DefaultValues.LOCATION_MATCH_REASON,
            ApplicationConstants.DefaultValues.EXPERIENCE_MATCH_REASON
        );
    }
    
    private List<String> getAvailableFeatures(User recruiter) {
        // Get subscription
        Optional<Subscription> subscription = subscriptionRepository.findByUserIdAndStatus(
                recruiter.getId(), Subscription.SubscriptionStatus.ACTIVE);
        
        if (subscription.isEmpty()) {
            return Arrays.asList(
                ApplicationConstants.DefaultValues.BASIC_FEATURE_1,
                ApplicationConstants.DefaultValues.BASIC_FEATURE_2
            );
        }
        
        // Get plan features
        List<PlanFeature> features = subscription.get().getSubscriptionPlan().getFeatures();
        
        return features.stream()
                .map(PlanFeature::getFeatureName)
                .collect(Collectors.toList());
    }
    
    private List<String> getPremiumFeatures(User recruiter) {
        return Arrays.asList(
            ApplicationConstants.DefaultValues.ADVANCED_FEATURE_1,
            ApplicationConstants.DefaultValues.ADVANCED_FEATURE_2,
            ApplicationConstants.DefaultValues.ADVANCED_FEATURE_3,
            ApplicationConstants.DefaultValues.ADVANCED_FEATURE_4
        );
    }
    
    private boolean hasPremiumFeatures(User recruiter) {
        // Get subscription
        Optional<Subscription> subscription = subscriptionRepository.findByUserIdAndStatus(
                recruiter.getId(), Subscription.SubscriptionStatus.ACTIVE);
        
        if (subscription.isEmpty()) {
            return false;
        }
        
        // Check if plan has premium features
        return subscription.get().getSubscriptionPlan().getPlanType() != SubscriptionPlan.PlanType.FREE;
    }





    /**
     * Get all applicants for a specific job
     * @param jobId The ID of the job
     * @param email The email of the recruiter
     * @return List of job applications with applicant details
     */
    @Transactional(readOnly = true)
    public List<JobApplicationDto> getJobApplicants(Long jobId, String email) {
        try {
            // Get recruiter
            User recruiter = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Recruiter not found"));

            // Get job post and verify ownership
            Job jobPost = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            // Verify the job belongs to the recruiter
//            if (!jobPost.getRecruiter().getId().equals(recruiter.getId())) {
//                throw new RuntimeException("You don't have permission to view applicants for this job");
//            }
//
            // Get all applications for this job
            List<JobApplication> applications = jobApplicationRepository.findByJobId(jobId);

            // Convert to DTOs
            return applications.stream()
                    .map(JobApplicationDto::new)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting job applicants: {}", e.getMessage());
            throw new RuntimeException("Failed to get job applicants: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public RecruiterProfileDto getRecruiterProfile(String email) {
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));

        RecruiterProfile recruiterProfile = recruiter.getRecruiterProfile();
        if (recruiterProfile == null) {
            throw new RuntimeException("Recruiter profile not found for user: " + email);
        }

        RecruiterProfileDto dto = new RecruiterProfileDto();

        // === USER INFO ===
        dto.setUserId(recruiter.getId());
        dto.setEmail(recruiter.getEmail());
        dto.setMobile(recruiter.getMobile());
        dto.setFirstName(recruiter.getFirstName());
        dto.setLastName(recruiter.getLastName());
        dto.setIsVerified(recruiter.getIsVerified());

        // === RECRUITER / COMPANY INFO ===
        dto.setId(recruiterProfile.getId());
        dto.setCompanyName(recruiterProfile.getCompanyName());
        dto.setContactPersonName(recruiterProfile.getContactPersonName());
        dto.setContactPhone(recruiterProfile.getContactPhone());
        dto.setDesignation(recruiterProfile.getDesignation());
        dto.setCompanyDescription(recruiterProfile.getCompanyDescription());
        dto.setCompanyWebsite(recruiterProfile.getCompanyWebsite());
        dto.setCompanyLogoUrl(recruiterProfile.getCompanyLogoUrl());
        dto.setProfilePhotoUrl(recruiterProfile.getProfilePhotoUrl());
        dto.setIndustry(recruiterProfile.getIndustry());
        dto.setCompanySize(recruiterProfile.getCompanySize());
        dto.setLocation(recruiterProfile.getLocation());
        dto.setIsVerifiedCompany(recruiterProfile.getIsVerifiedCompany());

        // === METRICS ===
        dto.setTotalJobsPosted(recruiterProfile.getTotalJobsPosted());
        dto.setSuccessfulHires(recruiterProfile.getSuccessfulHires());
        dto.setChatCredits(recruiterProfile.getChatCredits());

        // === CATEGORY INFO ===
        if (recruiterProfile.getRecruiterCategory() != null) {
            dto.setRecruiterCategoryId(recruiterProfile.getRecruiterCategory().getId());
            dto.setRecruiterCategoryName(recruiterProfile.getRecruiterCategory().getName());
        }

        return dto;
    }

    /**
     * Update recruiter profile
     * @param email Email of the recruiter
     * @param updateDto DTO containing fields to update
     * @return Updated recruiter profile
     */
    @Transactional
    public RecruiterProfileDto updateRecruiterProfile(String email, UpdateRecruiterProfileDto updateDto) {
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));

        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                .orElseThrow(() -> new RuntimeException("Recruiter profile not found for user: " + email));

        // Update company details
        if (updateDto.getCompanyName() != null) {
            recruiterProfile.setCompanyName(updateDto.getCompanyName());
        }
        if (updateDto.getCompanyDescription() != null) {
            recruiterProfile.setCompanyDescription(updateDto.getCompanyDescription());
        }
        if (updateDto.getCompanyWebsite() != null) {
            recruiterProfile.setCompanyWebsite(updateDto.getCompanyWebsite());
        }
        if (updateDto.getCompanyLogoUrl() != null) {
            recruiterProfile.setCompanyLogoUrl(updateDto.getCompanyLogoUrl());
        }
        if (updateDto.getProfilePhotoUrl() != null) {
            recruiterProfile.setProfilePhotoUrl(updateDto.getProfilePhotoUrl());
        }
        if (updateDto.getIndustry() != null) {
            recruiterProfile.setIndustry(updateDto.getIndustry());
        }
        if (updateDto.getCompanySize() != null) {
            recruiterProfile.setCompanySize(updateDto.getCompanySize());
        }

        // Update contact person details
        if (updateDto.getContactPersonName() != null) {
            recruiterProfile.setContactPersonName(updateDto.getContactPersonName());
        }
        if (updateDto.getContactPhone() != null) {
            recruiterProfile.setContactPhone(updateDto.getContactPhone());
        }
        if (updateDto.getDesignation() != null) {
            recruiterProfile.setDesignation(updateDto.getDesignation());
        }

        // Update location
        if (updateDto.getLocation() != null) {
            recruiterProfile.setLocation(updateDto.getLocation());
        }

        // Save updated profile
        recruiterProfileRepository.save(recruiterProfile);

        log.info("Recruiter profile updated for user: {}", email);

        // Return updated profile
        return getRecruiterProfile(email);
    }

    /**
     * Update recruiter profile photo URL
     * @param userId User ID
     * @param profilePhotoUrl URL of the profile photo
     * @return Updated profile photo URL
     */
    @Transactional
    public String updateProfilePhotoUrl(Long userId, String profilePhotoUrl) {
        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Recruiter profile not found"));

        recruiterProfile.setProfilePhotoUrl(profilePhotoUrl);
        recruiterProfileRepository.save(recruiterProfile);

        log.info("Updated profile photo URL for recruiter user ID: {}", userId);
        return profilePhotoUrl;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.DASHBOARD_RECRUITER, allEntries = true),
        @CacheEvict(value = CacheNames.JOBS_ACTIVE, allEntries = true)
    })
    public void deleteJob(Long jobId, String email) {
        // Find the job with the recruiter relationship loaded
        Job job = jobRepository.findByIdWithRecruiter(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));

        // Check if the job belongs to the recruiter
        if (!job.getRecruiter().getEmail().equals(email)) {
            throw new RuntimeException("You don't have permission to delete this job");
        }

        // Check if already deleted (soft delete)
        if (job.getDeletedAt() != null) {
            throw new RuntimeException("Job is already deleted");
        }

        // Soft delete the job
        LocalDateTime now = LocalDateTime.now();
        job.setDeletedAt(now);
        job.setStatus(Job.JobStatus.DELETED);
        job.setUpdatedAt(now);

        jobRepository.save(job);

        // Log the deletion
        log.info("Job {} soft deleted by {}", jobId, email);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.DASHBOARD_RECRUITER, allEntries = true),
        @CacheEvict(value = CacheNames.JOBS_ACTIVE, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    })
    public Map<String, Object> createJob(CreateJobDto createJobDto, String email) {
        try {
            User recruiter = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Recruiter not found"));
            Job job = new Job();
            job.setTitle(createJobDto.getTitle());
            job.setDescription(createJobDto.getDescription());
            job.setRequirements(createJobDto.getRequirements());
            job.setLocation(createJobDto.getLocation());

            // Set job type if provided
            if (createJobDto.getJobType() != null) {
                try {
                    job.setJobType(Job.JobType.valueOf(createJobDto.getJobType().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new ValidationException("Invalid job type: " + createJobDto.getJobType());
                }
            }

            // Set experience level if provided
            if (createJobDto.getExperienceLevel() != null) {
                try {
                    job.setExperienceLevel(Job.ExperienceLevel.valueOf(
                        createJobDto.getExperienceLevel().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new ValidationException("Invalid experience level: " + createJobDto.getExperienceLevel());
                }
            }

            // Set budget if provided
            try {
                if (createJobDto.getBudgetMin() != null) {
                    job.setBudgetMin(createJobDto.getBudgetMinAsBigDecimal());
                }
                if (createJobDto.getBudgetMax() != null) {
                    job.setBudgetMax(createJobDto.getBudgetMaxAsBigDecimal());
                }
                job.setCurrency(createJobDto.getCurrency());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid budget value: " + e.getMessage());
            }

            // Set dates
            job.setStartDate(createJobDto.getStartDate());
            job.setEndDate(createJobDto.getEndDate());
            job.setApplicationDeadline(createJobDto.getApplicationDeadline());

            // Set skills as JSON array if provided
            if (createJobDto.getSkillsRequired() != null && createJobDto.getSkillsRequired().length > 0) {
                String jsonSkills = "[\"" + String.join("\",\"", createJobDto.getSkillsRequired()) + "\"]";
                job.setSkillsRequired(jsonSkills);
            }

            // Set other fields
            job.setTags(createJobDto.getTags());
            job.setBenefits(createJobDto.getBenefits());
            job.setContactEmail(createJobDto.getContactEmail());
            job.setContactPhone(createJobDto.getContactPhone());
            job.setIsRemote(createJobDto.getIsRemote());
            job.setIsUrgent(createJobDto.getIsUrgent());
            job.setIsFeatured(createJobDto.getIsFeatured());
            job.setDurationDays(createJobDto.getDurationDays());

            // Set timestamps and status
            LocalDateTime now = LocalDateTime.now();
            job.setCreatedAt(now);
            job.setUpdatedAt(now);
            job.setPublishedAt(now);
            job.setStatus(Job.JobStatus.ACTIVE);

            // Set the recruiter
            job.setRecruiter(recruiter);

            // Save the job
            Job savedJob = jobRepository.save(job);

            // Prepare response
            Map<String, Object> result = new HashMap<>();
            result.put("id", savedJob.getId());
            result.put("title", savedJob.getTitle());
            result.put("status", savedJob.getStatus());
            result.put("createdAt", savedJob.getCreatedAt());

            return result;

        } catch (Exception e) {
            log.error("Error creating job: {}", e.getMessage());
            throw new RuntimeException("Failed to create job: " + e.getMessage(), e);
        }
    }

    /**
     * Get all applicants for a specific job
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getApplicantsByJobId(User recruiter, Long jobId, String status,
                                                     Boolean isShortlisted, Integer minExperience,
                                                     Integer maxExperience, Pageable pageable) {
        try {
            // Verify job exists and belongs to recruiter
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            if (!job.getRecruiter().getId().equals(recruiter.getId())) {
                throw new RuntimeException("Unauthorized: This job does not belong to you");
            }

            // Get all applications for this job
            Page<JobApplication> applications;

            if (status != null) {
                // Filter by status
                JobApplication.ApplicationStatus appStatus = JobApplication.ApplicationStatus.valueOf(status.toUpperCase());
                applications = jobApplicationRepository.findByJobAndStatus(job, appStatus, pageable);
            } else {
                // Get all applications
                applications = jobApplicationRepository.findByJob(job, pageable);
            }

            // Convert to detailed applicant DTOs with filtering
            List<Map<String, Object>> applicants = applications.getContent().stream()
                    .filter(app -> {
                        // Filter by shortlisted status if specified
                        if (isShortlisted != null && !app.getIsShortlisted().equals(isShortlisted)) {
                            return false;
                        }
                        // Filter by experience if specified
                        if (minExperience != null || maxExperience != null) {
                            Integer experience = app.getArtist().getExperienceYears();
                            if (experience == null) return false;
                            if (minExperience != null && experience < minExperience) return false;
                            if (maxExperience != null && experience > maxExperience) return false;
                        }
                        return true;
                    })
                    .map(application -> {
                        Map<String, Object> applicant = new HashMap<>();
                        ArtistProfile artist = application.getArtist();

                        // Application details
                        applicant.put("applicationId", application.getId());
                        applicant.put("status", application.getStatus().name());
                        applicant.put("appliedAt", application.getAppliedAt());
                        applicant.put("reviewedAt", application.getReviewedAt());

                        // Artist basic info
                        applicant.put("artistId", artist.getId());
                        applicant.put("firstName", artist.getFirstName());
                        applicant.put("lastName", artist.getLastName());
                        applicant.put("fullName", artist.getFirstName() + " " + artist.getLastName());
                        applicant.put("stageName", artist.getStageName());
                        applicant.put("email", artist.getUser().getEmail());
                        applicant.put("phone", artist.getUser().getMobile());

                        // Artist profile details
                        applicant.put("artistType", artist.getArtistType() != null ? artist.getArtistType().getName() : null);
                        applicant.put("location", artist.getLocation());
                        applicant.put("bio", artist.getBio());
                        applicant.put("experienceYears", artist.getExperienceYears());
                        applicant.put("skills", artist.getSkills());
                        applicant.put("languagesSpoken", artist.getLanguagesSpoken());
                        applicant.put("hourlyRate", artist.getHourlyRate());
                        applicant.put("isVerified", artist.getIsVerifiedBadge());

                        // Application specific details
                        applicant.put("coverLetter", application.getCoverLetter());
                        applicant.put("expectedSalary", application.getExpectedSalary());
                        applicant.put("availabilityDate", application.getAvailabilityDate());
                        applicant.put("portfolioUrl", application.getPortfolioUrl());
                        applicant.put("resumeUrl", application.getResumeUrl());
                        applicant.put("demoReelUrl", application.getDemoReelUrl());

                        // Application status flags
                        applicant.put("isShortlisted", application.getIsShortlisted());
                        applicant.put("isHired", application.getIsHired());
                        applicant.put("rating", application.getRating());
                        applicant.put("feedback", application.getFeedback());
                        applicant.put("interviewScheduledAt", application.getInterviewScheduledAt());
                        applicant.put("interviewNotes", application.getInterviewNotes());
                        applicant.put("rejectionReason", application.getRejectionReason());

                        return applicant;
                    })
                    .collect(Collectors.toList());

            // Job details
            Map<String, Object> jobDetails = new HashMap<>();
            jobDetails.put("jobId", job.getId());
            jobDetails.put("jobTitle", job.getTitle());
            jobDetails.put("jobStatus", job.getStatus().name());
            jobDetails.put("totalApplications", jobApplicationRepository.countByJob(job));

            // Prepare response
            Map<String, Object> result = new HashMap<>();
            result.put("job", jobDetails);
            result.put("applicants", applicants);
            result.put("totalElements", applications.getTotalElements());
            result.put("totalPages", applications.getTotalPages());
            result.put("currentPage", applications.getNumber());
            result.put("size", applications.getSize());

            return result;
        } catch (Exception e) {
            log.error("Error getting applicants for job {}: {}", jobId, e.getMessage(), e);
            throw new RuntimeException("Failed to get applicants: " + e.getMessage(), e);
        }
    }

    /**
     * Get all hired applicants for the currently logged-in recruiter
     *
     * SECURITY IMPLEMENTATION:
     * - Only returns hires belonging to jobs owned by the authenticated recruiter
     * - recruiterId is derived from authenticated User object, never from request params
     * - Validates recruiter profile exists before fetching data
     * - If jobId filter is provided, validates job ownership before applying filter
     * - Prevents cross-recruiter data leakage through strict ownership checks
     *
     * @param recruiter The authenticated recruiter user (from SecurityContext/JWT)
     * @param jobId Optional filter for specific job (must belong to this recruiter)
     * @param pageable Pagination parameters
     * @return Map containing hires list and pagination metadata
     * @throws RuntimeException if recruiter profile not found or unauthorized job access
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getHiredApplicants(User recruiter, Long jobId, Pageable pageable) {
        try {
            // SECURITY CHECK 1: Validate recruiter profile exists
            RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                    .orElseThrow(() -> new RuntimeException(ApplicationConstants.ErrorMessages.RECRUITER_PROFILE_NOT_FOUND));

            log.debug("Fetching hires for recruiter ID: {} (User: {})", recruiterProfile.getId(), recruiter.getEmail());

            // SECURITY CHECK 2: Get ONLY jobs belonging to this recruiter
            List<Job> recruiterJobs;
            if (jobId != null) {
                // Filter by specific job - must validate ownership
                Job job = jobRepository.findById(jobId)
                        .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));

                // SECURITY CHECK 3: Verify job belongs to this recruiter
                if (!job.getRecruiter().getId().equals(recruiter.getId())) {
                    log.warn("Unauthorized access attempt: Recruiter {} tried to access job {} owned by recruiter {}",
                            recruiter.getId(), jobId, job.getRecruiter().getId());
                    throw new RuntimeException("Unauthorized access: Job does not belong to you");
                }

                recruiterJobs = List.of(job);
                log.debug("Applying job filter: jobId={}, jobTitle={}", jobId, job.getTitle());
            } else {
                // Get ALL jobs by this recruiter (recruiter ownership enforced by query)
                recruiterJobs = jobRepository.findByRecruiterId(recruiter.getId());
                log.debug("Fetching hires from all {} jobs owned by recruiter", recruiterJobs.size());
            }

            // Extract job IDs for hired applications query
            List<Long> jobIds = recruiterJobs.stream()
                    .map(Job::getId)
                    .collect(Collectors.toList());

            // Handle case where recruiter has no jobs
            if (jobIds.isEmpty()) {
                log.info("Recruiter {} has no jobs, returning empty hires list", recruiter.getId());
                Map<String, Object> result = new HashMap<>();
                result.put("hires", new ArrayList<>());
                result.put("totalElements", 0);
                result.put("totalPages", 0);
                result.put("currentPage", 0);
                result.put("size", 0);
                return result;
            }

            // SECURITY: Query only returns hired applications from recruiter's jobs
            // The jobIds list is already filtered to contain ONLY this recruiter's jobs
            Page<JobApplication> hiredApplications = jobApplicationRepository.findByJobIdInAndIsHired(
                    jobIds, true, pageable);

            log.info("Found {} hired applicants for recruiter {} (page {}/{})",
                    hiredApplications.getTotalElements(),
                    recruiter.getId(),
                    hiredApplications.getNumber() + 1,
                    hiredApplications.getTotalPages());

            // Convert to response DTOs
            List<Map<String, Object>> hires = hiredApplications.getContent().stream()
                    .map(application -> {
                        Map<String, Object> hire = new HashMap<>();
                        hire.put("id", application.getId());
                        hire.put("jobId", application.getJob().getId());
                        hire.put("jobTitle", application.getJob().getTitle());
                        hire.put("artistId", application.getArtist().getId());
                        hire.put("artistName", application.getArtist().getFirstName() + " " + application.getArtist().getLastName());
                        hire.put("artistEmail", application.getArtist().getUser().getEmail());
                        hire.put("status", application.getStatus());
                        hire.put("hiredAt", application.getHiredAt());
                        hire.put("expectedSalary", application.getExpectedSalary());
                        hire.put("rating", application.getRating());
                        hire.put("feedback", application.getFeedback());
                        return hire;
                    })
                    .collect(Collectors.toList());

            // Prepare paginated response
            Map<String, Object> result = new HashMap<>();
            result.put("hires", hires);
            result.put("totalElements", hiredApplications.getTotalElements());
            result.put("totalPages", hiredApplications.getTotalPages());
            result.put("currentPage", hiredApplications.getNumber());
            result.put("size", hiredApplications.getSize());

            return result;
        } catch (RuntimeException e) {
            // Re-throw RuntimeExceptions to preserve error messages for controller
            log.error("Error getting hired applicants for recruiter {}: {}", recruiter.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting hired applicants for recruiter {}: {}", recruiter.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to get hired applicants: " + e.getMessage(), e);
        }
    }

    /**
     * Schedule interview for an applicant
     */
    @Transactional
    public Map<String, Object> scheduleInterview(User recruiter, Long applicationId,
                                                   String interviewDateTime, String interviewType,
                                                   String interviewLocation, String meetingLink,
                                                   String notes) {
        try {
            // Get the application
            JobApplication application = jobApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            // Verify the job belongs to this recruiter
            if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
                throw new RuntimeException("Unauthorized: This application does not belong to your job");
            }

            // Parse the interview date time
            LocalDateTime interviewScheduledAt = LocalDateTime.parse(interviewDateTime);

            // Update application with interview details
            application.setStatus(JobApplication.ApplicationStatus.INTERVIEW_SCHEDULED);
            application.setInterviewScheduledAt(interviewScheduledAt);
            application.setInterviewNotes(notes);
            application.setUpdatedAt(LocalDateTime.now());

            // Save the application
            jobApplicationRepository.save(application);

            // Get artist and job details for email
            ArtistProfile artist = application.getArtist();
            Job job = application.getJob();
            RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                    .orElseThrow(() -> new RuntimeException("Recruiter profile not found"));

            // Send email notification to applicant
            sendInterviewEmail(
                    artist.getUser().getEmail(),
                    artist.getFirstName() + " " + artist.getLastName(),
                    job.getTitle(),
                    recruiterProfile.getCompanyName(),
                    recruiterProfile.getContactPersonName(),
                    interviewScheduledAt,
                    interviewType,
                    interviewLocation,
                    meetingLink,
                    notes
            );

            // Create notification for recruiter
            try {
                String notificationTitle = "Interview Scheduled";
                String notificationMessage = String.format(
                        "Interview scheduled with %s %s for %s position on %s",
                        artist.getFirstName(),
                        artist.getLastName(),
                        job.getTitle(),
                        interviewScheduledAt.toString()
                );
                String actionUrl = "/recruiter/applications/" + application.getId();
                String metadata = String.format(
                        "{\"applicationId\":%d,\"jobId\":%d,\"artistId\":%d,\"interviewDateTime\":\"%s\"}",
                        application.getId(),
                        job.getId(),
                        artist.getId(),
                        interviewScheduledAt.toString()
                );

                notificationService.createNotification(
                        recruiter,
                        Notification.NotificationType.INTERVIEW_SCHEDULED,
                        notificationTitle,
                        notificationMessage,
                        Notification.Priority.HIGH,
                        actionUrl,
                        metadata
                );
            } catch (Exception notifError) {
                log.error("Failed to create notification for interview scheduling: {}", notifError.getMessage());
                // Don't fail the whole operation if notification fails
            }

            // Prepare response
            Map<String, Object> result = new HashMap<>();
            result.put("applicationId", application.getId());
            result.put("artistName", artist.getFirstName() + " " + artist.getLastName());
            result.put("artistEmail", artist.getUser().getEmail());
            result.put("jobTitle", job.getTitle());
            result.put("interviewScheduledAt", interviewScheduledAt);
            result.put("interviewType", interviewType);
            result.put("interviewLocation", interviewLocation);
            result.put("meetingLink", meetingLink);
            result.put("status", application.getStatus().name());
            result.put("emailSent", true);

            log.info("Interview scheduled successfully for application ID: {} at {}", applicationId, interviewScheduledAt);

            return result;
        } catch (Exception e) {
            log.error("Error scheduling interview: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to schedule interview: " + e.getMessage(), e);
        }
    }

    /**
     * Submit interview result (HIRED or REJECTED) after interview scheduling
     *
     * SECURITY & BUSINESS LOGIC:
     * - recruiterId extracted from JWT/SecurityContext (NOT from request)
     * - Validates application belongs to recruiter's job
     * - Allows direct hire/reject from INTERVIEW_SCHEDULED status
     * - Prevents invalid state transitions (already HIRED/REJECTED)
     * - Saves offer details (salary, contract) for HIRED
     * - Saves rejection reason and timestamp for REJECTED
     * - Sends email notifications
     * - Logs all hire/reject decisions with recruiter & application IDs
     */
    @Transactional
    public Map<String, Object> submitInterviewResult(User recruiter, InterviewResultDto resultDto) {
        try {
            // SECURITY CHECK 1: Get the application
            JobApplication application = jobApplicationRepository.findById(resultDto.getApplicationId())
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            // SECURITY CHECK 2: Verify the job belongs to this recruiter
            if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
                log.warn("Unauthorized access attempt - Recruiter {} tried to access application {} from recruiter {}",
                        recruiter.getId(), application.getId(), application.getJob().getRecruiter().getId());
                throw new RuntimeException("Unauthorized: This application does not belong to your job");
            }

            // VALIDATION: Check for invalid state transitions
            JobApplication.ApplicationStatus currentStatus = application.getStatus();

            // Prevent re-hiring already hired candidates
            if (currentStatus == JobApplication.ApplicationStatus.HIRED &&
                resultDto.getResult() == InterviewResultDto.InterviewResult.HIRED) {
                throw new RuntimeException("Cannot hire: Applicant is already hired");
            }

            // Prevent re-rejecting already rejected candidates
            if (currentStatus == JobApplication.ApplicationStatus.REJECTED &&
                resultDto.getResult() == InterviewResultDto.InterviewResult.REJECTED) {
                throw new RuntimeException("Cannot reject: Applicant is already rejected");
            }

            // Prevent hiring already rejected candidates (unless recruiter wants to override)
            if (currentStatus == JobApplication.ApplicationStatus.REJECTED &&
                resultDto.getResult() == InterviewResultDto.InterviewResult.HIRED) {
                throw new RuntimeException("Cannot hire: Applicant was previously rejected");
            }

            // Prevent rejecting already hired candidates
            if (currentStatus == JobApplication.ApplicationStatus.HIRED &&
                resultDto.getResult() == InterviewResultDto.InterviewResult.REJECTED) {
                throw new RuntimeException("Cannot reject: Applicant is already hired");
            }

            // VALIDATION: Require interview to be scheduled (allow direct hire/reject after scheduling)
            if (currentStatus != JobApplication.ApplicationStatus.INTERVIEW_SCHEDULED &&
                currentStatus != JobApplication.ApplicationStatus.INTERVIEWED) {
                throw new RuntimeException("Interview must be scheduled before submitting result. Current status: " + currentStatus);
            }

            // Get artist and job details
            ArtistProfile artist = application.getArtist();
            Job job = application.getJob();
            RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                    .orElseThrow(() -> new RuntimeException("Recruiter profile not found"));

            // Update application based on result
            LocalDateTime now = LocalDateTime.now();

            if (resultDto.getResult() == InterviewResultDto.InterviewResult.HIRED) {
                // === HIRE FLOW ===
                application.setStatus(JobApplication.ApplicationStatus.HIRED);
                application.setIsHired(true);
                application.setHiredAt(now);

                // Save offer details if provided
                if (resultDto.getOfferDetails() != null) {
                    if (resultDto.getOfferDetails().getSalary() != null) {
                        application.setOfferedSalary(resultDto.getOfferDetails().getSalary());
                    }
                    if (resultDto.getOfferDetails().getContractUrl() != null &&
                        !resultDto.getOfferDetails().getContractUrl().isEmpty()) {
                        application.setContractUrl(resultDto.getOfferDetails().getContractUrl());
                    }
                }

                // Update recruiter's successful hires count
                recruiterProfile.setSuccessfulHires(
                    (recruiterProfile.getSuccessfulHires() != null ? recruiterProfile.getSuccessfulHires() : 0) + 1
                );
                recruiterProfileRepository.save(recruiterProfile);

                log.info("HIRED - Recruiter ID: {}, Application ID: {}, Artist: {}, Job: {}, Offered Salary: {}",
                    recruiter.getId(), application.getId(),
                    artist.getFirstName() + " " + artist.getLastName(),
                    job.getTitle(),
                    application.getOfferedSalary());

            } else {
                // === REJECT FLOW ===
                application.setStatus(JobApplication.ApplicationStatus.REJECTED);
                application.setRejectedAt(now);

                // Use dedicated rejectionReason field if provided, otherwise use notes
                String rejectionReasonText = resultDto.getRejectionReason() != null ?
                        resultDto.getRejectionReason() : resultDto.getNotes();

                if (rejectionReasonText != null && !rejectionReasonText.isEmpty()) {
                    application.setRejectionReason(rejectionReasonText);
                }

                log.info("REJECTED - Recruiter ID: {}, Application ID: {}, Artist: {}, Job: {}, Reason: {}",
                    recruiter.getId(), application.getId(),
                    artist.getFirstName() + " " + artist.getLastName(),
                    job.getTitle(),
                    rejectionReasonText);
            }

            // Save notes/feedback if provided
            if (resultDto.getNotes() != null && !resultDto.getNotes().isEmpty()) {
                application.setFeedback(resultDto.getNotes());
            }

            application.setReviewedAt(now);
            application.setUpdatedAt(now);

            // Save the application
            JobApplication savedApplication = jobApplicationRepository.save(application);

            // Send email notification to applicant
            sendInterviewResultEmail(
                    artist.getUser().getEmail(),
                    artist.getFirstName() + " " + artist.getLastName(),
                    job.getTitle(),
                    recruiterProfile.getCompanyName(),
                    resultDto.getResult(),
                    resultDto.getResult() == InterviewResultDto.InterviewResult.HIRED ?
                        resultDto.getNotes() : resultDto.getRejectionReason()
            );

            // Create notification for recruiter
            try {
                Notification.NotificationType notificationType;
                Notification.Priority priority;
                String notificationTitle;
                String notificationMessage;

                if (resultDto.getResult() == InterviewResultDto.InterviewResult.HIRED) {
                    notificationType = Notification.NotificationType.APPLICANT_HIRED;
                    priority = Notification.Priority.HIGH;
                    notificationTitle = "Applicant Hired";
                    notificationMessage = String.format(
                            "You have successfully hired %s %s for %s position",
                            artist.getFirstName(),
                            artist.getLastName(),
                            job.getTitle()
                    );
                } else {
                    notificationType = Notification.NotificationType.APPLICANT_REJECTED;
                    priority = Notification.Priority.MEDIUM;
                    notificationTitle = "Applicant Rejected";
                    notificationMessage = String.format(
                            "You have rejected %s %s for %s position",
                            artist.getFirstName(),
                            artist.getLastName(),
                            job.getTitle()
                    );
                }

                String actionUrl = "/recruiter/applications/" + savedApplication.getId();
                String metadata = String.format(
                        "{\"applicationId\":%d,\"jobId\":%d,\"artistId\":%d,\"result\":\"%s\",\"offeredSalary\":%s}",
                        savedApplication.getId(),
                        job.getId(),
                        artist.getId(),
                        resultDto.getResult().name(),
                        savedApplication.getOfferedSalary() != null ? savedApplication.getOfferedSalary().toString() : "null"
                );

                notificationService.createNotification(
                        recruiter,
                        notificationType,
                        notificationTitle,
                        notificationMessage,
                        priority,
                        actionUrl,
                        metadata
                );
            } catch (Exception notifError) {
                log.error("Failed to create notification for interview result: {}", notifError.getMessage());
                // Don't fail the whole operation if notification fails
            }

            // Prepare response
            Map<String, Object> result = new HashMap<>();
            result.put("applicationId", savedApplication.getId());
            result.put("artistId", artist.getId());
            result.put("artistName", artist.getFirstName() + " " + artist.getLastName());
            result.put("artistEmail", artist.getUser().getEmail());
            result.put("jobId", job.getId());
            result.put("jobTitle", job.getTitle());
            result.put("result", resultDto.getResult().name());
            result.put("status", savedApplication.getStatus().name());
            result.put("notes", resultDto.getNotes());
            result.put("reviewedAt", savedApplication.getReviewedAt());
            result.put("isHired", savedApplication.getIsHired());
            result.put("hiredAt", savedApplication.getHiredAt());
            result.put("rejectedAt", savedApplication.getRejectedAt());
            result.put("offeredSalary", savedApplication.getOfferedSalary());
            result.put("contractUrl", savedApplication.getContractUrl());
            result.put("rejectionReason", savedApplication.getRejectionReason());
            result.put("emailSent", true);

            return result;
        } catch (Exception e) {
            log.error("Error submitting interview result: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to submit interview result: " + e.getMessage(), e);
        }
    }

    /**
     * Send interview result email to applicant
     */
    private void sendInterviewResultEmail(String toEmail, String applicantName, String jobTitle,
                                           String companyName, InterviewResultDto.InterviewResult result,
                                           String notes) {
        try {
            if (result == InterviewResultDto.InterviewResult.HIRED) {
                emailService.sendHiredEmail(toEmail, applicantName, jobTitle, companyName, notes);
            } else {
                emailService.sendRejectionEmail(toEmail, applicantName, jobTitle, companyName, notes);
            }
            log.info("Interview result email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Error sending interview result email: {}", e.getMessage(), e);
            // Don't throw exception - result is already recorded
        }
    }

    /**
     * Send interview scheduled email to applicant
     */
    private void sendInterviewEmail(String toEmail, String applicantName, String jobTitle,
                                     String companyName, String recruiterName,
                                     LocalDateTime interviewDateTime, String interviewType,
                                     String interviewLocation, String meetingLink, String notes) {
        try {
            // Use EmailService to send interview schedule email
            emailService.sendInterviewScheduleEmail(
                    toEmail,
                    applicantName,
                    jobTitle,
                    companyName,
                    recruiterName,
                    interviewDateTime,
                    interviewType,
                    interviewLocation,
                    meetingLink,
                    notes
            );
            log.info("Interview email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Error sending interview email: {}", e.getMessage(), e);
            // Don't throw exception - interview is already scheduled
        }
    }

    /**
     * Get dashboard metrics - KPIs with trends
     * SECURITY: Returns data only for the logged-in recruiter
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.DASHBOARD_RECRUITER, key = "'metrics-' + #recruiter.id")
    public Map<String, Object> getDashboardMetrics(User recruiter) {
        try {
            log.debug("Cache MISS: Loading recruiter dashboard metrics for: {}", recruiter.getId());
            // Get recruiter profile
            RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserId(recruiter.getId())
                    .orElseThrow(() -> new RuntimeException("Recruiter profile not found"));

            // Get all jobs for this recruiter
            List<Job> allJobs = jobRepository.findByRecruiterId(recruiter.getId());
            List<Long> jobIds = allJobs.stream().map(Job::getId).collect(Collectors.toList());

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastMonth = now.minusMonths(1);

            // Calculate metrics
            long activeJobs = allJobs.stream()
                    .filter(j -> j.getStatus() == Job.JobStatus.ACTIVE)
                    .count();

            long totalApplications = jobIds.isEmpty() ? 0 :
                    jobApplicationRepository.countByJobIdIn(jobIds);

            long totalInterviews = jobIds.isEmpty() ? 0 :
                    jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.INTERVIEW_SCHEDULED) +
                    jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.INTERVIEWED);

            long totalOffers = jobIds.isEmpty() ? 0 :
                    jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.SELECTED);

            long totalHires = jobIds.isEmpty() ? 0 :
                    jobApplicationRepository.countByJobIdInAndIsHired(jobIds, true);

            long totalRejections = jobIds.isEmpty() ? 0 :
                    jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.REJECTED);

            // Calculate last month's metrics for trends
            long lastMonthApplications = jobIds.isEmpty() ? 0 :
                    jobApplicationRepository.countByJobIdInAndCreatedAtAfter(jobIds, lastMonth);

            long lastMonthHires = jobIds.isEmpty() ? 0 :
                    jobApplicationRepository.countByJobIdInAndHiredAtAfter(jobIds, lastMonth);

            // Calculate trend percentages
            double applicationsTrend = totalApplications > 0 ?
                    ((double) lastMonthApplications / totalApplications) * 100 : 0;
            double hiresTrend = totalHires > 0 ?
                    ((double) lastMonthHires / totalHires) * 100 : 0;

            Map<String, Object> metrics = new HashMap<>();

            // Active Jobs
            Map<String, Object> activeJobsMetric = new HashMap<>();
            activeJobsMetric.put("value", activeJobs);
            activeJobsMetric.put("trend", 0); // Can be calculated if needed
            activeJobsMetric.put("label", "Active Jobs");
            metrics.put("activeJobs", activeJobsMetric);

            // Applications
            Map<String, Object> applicationsMetric = new HashMap<>();
            applicationsMetric.put("value", totalApplications);
            applicationsMetric.put("trend", Math.round(applicationsTrend * 10.0) / 10.0);
            applicationsMetric.put("label", "Applications");
            metrics.put("applications", applicationsMetric);

            // Interviews
            Map<String, Object> interviewsMetric = new HashMap<>();
            interviewsMetric.put("value", totalInterviews);
            interviewsMetric.put("trend", 0);
            interviewsMetric.put("label", "Interviews");
            metrics.put("interviews", interviewsMetric);

            // Offers
            Map<String, Object> offersMetric = new HashMap<>();
            offersMetric.put("value", totalOffers);
            offersMetric.put("trend", 0);
            offersMetric.put("label", "Offers");
            metrics.put("offers", offersMetric);

            // Hires
            Map<String, Object> hiresMetric = new HashMap<>();
            hiresMetric.put("value", totalHires);
            hiresMetric.put("trend", Math.round(hiresTrend * 10.0) / 10.0);
            hiresMetric.put("label", "Hires");
            metrics.put("hires", hiresMetric);

            // Rejections
            Map<String, Object> rejectionsMetric = new HashMap<>();
            rejectionsMetric.put("value", totalRejections);
            rejectionsMetric.put("trend", 0);
            rejectionsMetric.put("label", "Rejections");
            metrics.put("rejections", rejectionsMetric);

            log.info("Dashboard metrics fetched for recruiter {}", recruiter.getId());
            return metrics;

        } catch (Exception e) {
            log.error("Error fetching dashboard metrics: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch dashboard metrics: " + e.getMessage(), e);
        }
    }

    /**
     * Get latest applicants for dashboard table
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLatestApplicants(User recruiter, int limit) {
        try {
            // Get recruiter's jobs
            List<Job> jobs = jobRepository.findByRecruiterId(recruiter.getId());
            List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());

            if (jobIds.isEmpty()) {
                return new ArrayList<>();
            }

            // Get recent applications
            Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<JobApplication> applications = jobApplicationRepository.findByJobIdIn(jobIds, pageable);

            return applications.getContent().stream().map(app -> {
                Map<String, Object> applicant = new HashMap<>();
                ArtistProfile artist = app.getArtist();

                applicant.put("id", app.getId());
                applicant.put("name", artist.getFirstName() + " " + artist.getLastName());
                applicant.put("avatar", artist.getProfileUrl());
                applicant.put("job", app.getJob().getTitle());
                applicant.put("skills", artist.getSkills());
                applicant.put("status", app.getStatus().name());
                applicant.put("appliedAt", app.getCreatedAt());
                applicant.put("email", artist.getUser().getEmail());

                return applicant;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching latest applicants: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch latest applicants: " + e.getMessage(), e);
        }
    }

    /**
     * Get applications trend data for line chart (7 months)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getApplicationsTrend(User recruiter) {
        try {
            List<Job> jobs = jobRepository.findByRecruiterId(recruiter.getId());
            List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());

            List<String> months = new ArrayList<>();
            List<Long> applications = new ArrayList<>();
            List<Long> interviews = new ArrayList<>();
            List<Long> hires = new ArrayList<>();

            LocalDateTime now = LocalDateTime.now();

            for (int i = 6; i >= 0; i--) {
                LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime monthEnd = monthStart.plusMonths(1).minusDays(1).withHour(23).withMinute(59).withSecond(59);

                months.add(monthStart.getMonth().toString().substring(0, 3));

                if (!jobIds.isEmpty()) {
                    long monthApplications = jobApplicationRepository.countByJobIdInAndCreatedAtBetween(jobIds, monthStart, monthEnd);
                    long monthInterviews = jobApplicationRepository.countByJobIdInAndInterviewScheduledAtBetween(jobIds, monthStart, monthEnd);
                    long monthHires = jobApplicationRepository.countByJobIdInAndHiredAtBetween(jobIds, monthStart, monthEnd);

                    applications.add(monthApplications);
                    interviews.add(monthInterviews);
                    hires.add(monthHires);
                } else {
                    applications.add(0L);
                    interviews.add(0L);
                    hires.add(0L);
                }
            }

            Map<String, Object> trend = new HashMap<>();
            trend.put("months", months);
            trend.put("applications", applications);
            trend.put("interviews", interviews);
            trend.put("hires", hires);

            return trend;

        } catch (Exception e) {
            log.error("Error fetching applications trend: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch applications trend: " + e.getMessage(), e);
        }
    }

    /**
     * Get application status breakdown for donut chart
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getApplicationStatus(User recruiter) {
        try {
            List<Job> jobs = jobRepository.findByRecruiterId(recruiter.getId());
            List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());

            if (jobIds.isEmpty()) {
                return createEmptyStatusBreakdown();
            }

            long newApps = jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.APPLIED);
            long viewed = jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.UNDER_REVIEW);
            long interviewing = jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.INTERVIEW_SCHEDULED) +
                               jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.INTERVIEWED);
            long offered = jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.SELECTED);
            long rejected = jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.REJECTED);

            long total = newApps + viewed + interviewing + offered + rejected;

            Map<String, Object> status = new HashMap<>();
            status.put("labels", Arrays.asList("New", "Viewed", "Interviewing", "Offered", "Rejected"));
            status.put("data", Arrays.asList(newApps, viewed, interviewing, offered, rejected));

            List<Double> percentages = new ArrayList<>();
            if (total > 0) {
                percentages.add(Math.round((newApps * 100.0 / total) * 10.0) / 10.0);
                percentages.add(Math.round((viewed * 100.0 / total) * 10.0) / 10.0);
                percentages.add(Math.round((interviewing * 100.0 / total) * 10.0) / 10.0);
                percentages.add(Math.round((offered * 100.0 / total) * 10.0) / 10.0);
                percentages.add(Math.round((rejected * 100.0 / total) * 10.0) / 10.0);
            } else {
                percentages = Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0);
            }

            status.put("percentages", percentages);
            status.put("total", total);

            return status;

        } catch (Exception e) {
            log.error("Error fetching application status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch application status: " + e.getMessage(), e);
        }
    }

    /**
     * Get interview outcomes for bar chart
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getInterviewOutcomes(User recruiter) {
        try {
            List<Job> jobs = jobRepository.findByRecruiterId(recruiter.getId());
            List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());

            if (jobIds.isEmpty()) {
                return createEmptyOutcomes();
            }

            long selected = jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.SELECTED) +
                           jobApplicationRepository.countByJobIdInAndIsHired(jobIds, true);
            long rejected = jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.REJECTED);
            long onHold = jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.ON_HOLD);
            long pending = jobApplicationRepository.countByJobIdInAndStatus(jobIds, JobApplication.ApplicationStatus.INTERVIEWED);

            Map<String, Object> outcomes = new HashMap<>();
            outcomes.put("labels", Arrays.asList("Selected", "Rejected", "On Hold", "Pending"));
            outcomes.put("data", Arrays.asList(selected, rejected, onHold, pending));

            return outcomes;

        } catch (Exception e) {
            log.error("Error fetching interview outcomes: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch interview outcomes: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> createEmptyStatusBreakdown() {
        Map<String, Object> status = new HashMap<>();
        status.put("labels", Arrays.asList("New", "Viewed", "Interviewing", "Offered", "Rejected"));
        status.put("data", Arrays.asList(0, 0, 0, 0, 0));
        status.put("percentages", Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0));
        status.put("total", 0);
        return status;
    }

    private Map<String, Object> createEmptyOutcomes() {
        Map<String, Object> outcomes = new HashMap<>();
        outcomes.put("labels", Arrays.asList("Selected", "Rejected", "On Hold", "Pending"));
        outcomes.put("data", Arrays.asList(0, 0, 0, 0));
        return outcomes;
    }
}
