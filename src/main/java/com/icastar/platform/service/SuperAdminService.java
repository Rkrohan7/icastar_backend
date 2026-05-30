package com.icastar.platform.service;

import com.icastar.platform.dto.superadmin.*;
import com.icastar.platform.entity.*;
import com.icastar.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final AuditionRepository auditionRepository;
    private final HireRequestRepository hireRequestRepository;
    private final CastingCallRepository castingCallRepository;
    private final ArtistTypeRepository artistTypeRepository;

    /**
     * Get comprehensive dashboard statistics
     */
    @Transactional(readOnly = true)
    public SuperAdminDashboardDto getDashboardStats() {
        log.info("Generating super admin dashboard statistics");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.with(LocalTime.MIN);
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);

        // User Statistics
        Long totalUsers = userRepository.count();
        Long totalArtists = userRepository.countByRole(User.UserRole.ARTIST);
        Long totalRecruiters = userRepository.countByRole(User.UserRole.RECRUITER);
        Long totalAdmins = userRepository.countByRole(User.UserRole.ADMIN);
        Long activeUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        Long inactiveUsers = userRepository.countByStatus(User.UserStatus.INACTIVE);
        Long suspendedUsers = userRepository.countByStatus(User.UserStatus.SUSPENDED);
        Long bannedUsers = userRepository.countByStatus(User.UserStatus.BANNED);
        Long verifiedUsers = userRepository.countByIsVerified(true);
        Long unverifiedUsers = userRepository.countByIsVerified(false);

        Long newUsersToday = userRepository.countByRoleAndCreatedAtAfter(null, startOfDay);
        Long newUsersThisWeek = (long) userRepository.findByCreatedAtAfter(startOfWeek).size();
        Long newUsersThisMonth = (long) userRepository.findByCreatedAtAfter(startOfMonth).size();

        // Job Statistics
        Long totalJobs = jobRepository.count();
        Long activeJobs = jobRepository.countByStatus(Job.JobStatus.ACTIVE);
        Long closedJobs = jobRepository.countByStatus(Job.JobStatus.CLOSED);
        Long draftJobs = jobRepository.countByStatus(Job.JobStatus.DRAFT);
        Long featuredJobs = (long) jobRepository.findByIsFeaturedTrue().size();

        // Application Statistics
        Long totalApplications = jobApplicationRepository.count();
        Long pendingApplications = jobApplicationRepository.countByStatus(JobApplication.ApplicationStatus.APPLIED);
        Long acceptedApplications = jobApplicationRepository.countByStatus(JobApplication.ApplicationStatus.SELECTED);
        Long rejectedApplications = jobApplicationRepository.countByStatus(JobApplication.ApplicationStatus.REJECTED);
        Long shortlistedApplications = jobApplicationRepository.countByStatus(JobApplication.ApplicationStatus.SHORTLISTED);

        // Get top recruiters
        List<SuperAdminDashboardDto.TopRecruiterDto> topRecruiters = getTopRecruiters(5);

        // Get top artists
        List<SuperAdminDashboardDto.TopArtistDto> topArtists = getTopArtists(5);

        // Get top jobs
        List<SuperAdminDashboardDto.TopJobDto> topJobs = getTopJobs(5);

        // Get artist type distribution
        Map<String, Long> artistTypeDistribution = getArtistTypeDistribution();

        // Get job type distribution
        Map<String, Long> jobTypeDistribution = getJobTypeDistribution();

        return SuperAdminDashboardDto.builder()
                .totalUsers(totalUsers)
                .totalArtists(totalArtists)
                .totalRecruiters(totalRecruiters)
                .totalAdmins(totalAdmins)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .suspendedUsers(suspendedUsers)
                .bannedUsers(bannedUsers)
                .verifiedUsers(verifiedUsers)
                .unverifiedUsers(unverifiedUsers)
                .newUsersToday(newUsersToday != null ? newUsersToday : 0L)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .totalJobs(totalJobs)
                .activeJobs(activeJobs)
                .closedJobs(closedJobs)
                .draftJobs(draftJobs)
                .featuredJobs(featuredJobs)
                .totalApplications(totalApplications)
                .pendingApplications(pendingApplications)
                .acceptedApplications(acceptedApplications)
                .rejectedApplications(rejectedApplications)
                .shortlistedApplications(shortlistedApplications)
                .topRecruiters(topRecruiters)
                .topArtists(topArtists)
                .topJobs(topJobs)
                .artistTypeDistribution(artistTypeDistribution)
                .jobTypeDistribution(jobTypeDistribution)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Get all recruiters with detailed information
     */
    @Transactional(readOnly = true)
    public Page<AllRecruitersResponseDto> getAllRecruiters(Pageable pageable, String search, String status, String category) {
        log.info("Fetching all recruiters - search: {}, status: {}, category: {}", search, status, category);

        Page<RecruiterProfile> recruiters = recruiterProfileRepository.findAll(pageable);

        return recruiters.map(this::mapToRecruiterDto);
    }

    /**
     * Get all artists with detailed information
     */
    @Transactional(readOnly = true)
    public Page<AllArtistsResponseDto> getAllArtists(Pageable pageable, String search, String status, String artistType) {
        log.info("Fetching all artists - search: {}, status: {}, artistType: {}", search, status, artistType);

        Page<ArtistProfile> artists = artistProfileRepository.findAll(pageable);

        return artists.map(this::mapToArtistDto);
    }

    /**
     * Get all jobs with detailed information
     */
    @Transactional(readOnly = true)
    public Page<Job> getAllJobs(Pageable pageable, String search, String status, String jobType) {
        log.info("Fetching all jobs - search: {}, status: {}, jobType: {}", search, status, jobType);

        if (status != null && !status.isEmpty()) {
            try {
                Job.JobStatus jobStatus = Job.JobStatus.valueOf(status.toUpperCase());
                return jobRepository.findByStatus(jobStatus, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid job status: {}", status);
            }
        }

        return jobRepository.findAll(pageable);
    }

    /**
     * Get user report
     */
    @Transactional(readOnly = true)
    public ReportsDto.UserReportDto getUserReport(LocalDate startDate, LocalDate endDate) {
        log.info("Generating user report from {} to {}", startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<User> usersInPeriod = userRepository.findByCreatedAtAfter(start);
        usersInPeriod = usersInPeriod.stream()
                .filter(u -> u.getCreatedAt().isBefore(end))
                .collect(Collectors.toList());

        Long newArtists = usersInPeriod.stream()
                .filter(u -> u.getRole() == User.UserRole.ARTIST)
                .count();

        Long newRecruiters = usersInPeriod.stream()
                .filter(u -> u.getRole() == User.UserRole.RECRUITER)
                .count();

        return ReportsDto.UserReportDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalNewUsers((long) usersInPeriod.size())
                .newArtists(newArtists)
                .newRecruiters(newRecruiters)
                .activeUsers(userRepository.countByStatus(User.UserStatus.ACTIVE))
                .inactiveUsers(userRepository.countByStatus(User.UserStatus.INACTIVE))
                .verifiedUsers(userRepository.countByIsVerified(true))
                .suspendedUsers(userRepository.countByStatus(User.UserStatus.SUSPENDED))
                .bannedUsers(userRepository.countByStatus(User.UserStatus.BANNED))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Get job report
     */
    @Transactional(readOnly = true)
    public ReportsDto.JobReportDto getJobReport(LocalDate startDate, LocalDate endDate) {
        log.info("Generating job report from {} to {}", startDate, endDate);

        Long totalJobs = jobRepository.count();
        Long activeJobs = jobRepository.countByStatus(Job.JobStatus.ACTIVE);
        Long closedJobs = jobRepository.countByStatus(Job.JobStatus.CLOSED);

        Long totalApplications = jobApplicationRepository.count();
        Long totalHires = (long) jobApplicationRepository.findByIsHiredTrue().size();

        Double averageApplicationsPerJob = totalJobs > 0 ? (double) totalApplications / totalJobs : 0.0;
        Double conversionRate = totalApplications > 0 ? (double) totalHires / totalApplications * 100 : 0.0;

        Map<String, Long> applicationsByStatus = new HashMap<>();
        for (JobApplication.ApplicationStatus status : JobApplication.ApplicationStatus.values()) {
            applicationsByStatus.put(status.name(), jobApplicationRepository.countByStatus(status));
        }

        return ReportsDto.JobReportDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalJobsPosted(totalJobs)
                .activeJobs(activeJobs)
                .closedJobs(closedJobs)
                .featuredJobs((long) jobRepository.findByIsFeaturedTrue().size())
                .totalApplicationsReceived(totalApplications)
                .averageApplicationsPerJob(averageApplicationsPerJob)
                .totalHiresMade(totalHires)
                .conversionRate(conversionRate)
                .applicationsByStatus(applicationsByStatus)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // Helper methods

    private List<SuperAdminDashboardDto.TopRecruiterDto> getTopRecruiters(int limit) {
        List<RecruiterProfile> recruiters = recruiterProfileRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        return recruiters.stream()
                .map(r -> SuperAdminDashboardDto.TopRecruiterDto.builder()
                        .id(r.getId())
                        .name(r.getUser() != null ? r.getUser().getFirstName() + " " + r.getUser().getLastName() : "N/A")
                        .companyName(r.getCompanyName())
                        .email(r.getUser() != null ? r.getUser().getEmail() : "N/A")
                        .totalJobsPosted(r.getUser() != null ? jobRepository.countByRecruiter(r.getUser()) : 0L)
                        .build())
                .collect(Collectors.toList());
    }

    private List<SuperAdminDashboardDto.TopArtistDto> getTopArtists(int limit) {
        List<ArtistProfile> artists = artistProfileRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        return artists.stream()
                .map(a -> SuperAdminDashboardDto.TopArtistDto.builder()
                        .id(a.getId())
                        .name(a.getStageName() != null ? a.getStageName() :
                              (a.getFirstName() != null ? a.getFirstName() : "N/A"))
                        .artistType(a.getArtistType() != null ? a.getArtistType().getName() : "N/A")
                        .email(a.getUser() != null ? a.getUser().getEmail() : "N/A")
                        .totalApplications(jobApplicationRepository.countByArtistId(a.getId()))
                        .profileViews(a.getTotalApplications() != null ? a.getTotalApplications().longValue() : 0L)
                        .build())
                .collect(Collectors.toList());
    }

    private List<SuperAdminDashboardDto.TopJobDto> getTopJobs(int limit) {
        List<Job> jobs = jobRepository.findMostPopularJobs(PageRequest.of(0, limit));

        return jobs.stream()
                .map(j -> SuperAdminDashboardDto.TopJobDto.builder()
                        .id(j.getId())
                        .title(j.getTitle())
                        .recruiterName(j.getRecruiter() != null ?
                            j.getRecruiter().getFirstName() + " " + j.getRecruiter().getLastName() : "N/A")
                        .applicationCount(j.getApplicationsCount() != null ? j.getApplicationsCount().longValue() : 0L)
                        .viewCount(j.getViewsCount() != null ? j.getViewsCount().longValue() : 0L)
                        .status(j.getStatus() != null ? j.getStatus().name() : "N/A")
                        .build())
                .collect(Collectors.toList());
    }

    private Map<String, Long> getArtistTypeDistribution() {
        Map<String, Long> distribution = new HashMap<>();
        List<ArtistType> artistTypes = artistTypeRepository.findAll();

        for (ArtistType type : artistTypes) {
            Long count = artistProfileRepository.countByArtistTypeId(type.getId());
            distribution.put(type.getName(), count != null ? count : 0L);
        }

        return distribution;
    }

    private Map<String, Long> getJobTypeDistribution() {
        Map<String, Long> distribution = new HashMap<>();

        for (Job.JobType type : Job.JobType.values()) {
            Long count = (long) jobRepository.findByJobType(type).size();
            distribution.put(type.name(), count);
        }

        return distribution;
    }

    private AllRecruitersResponseDto mapToRecruiterDto(RecruiterProfile recruiter) {
        User user = recruiter.getUser();

        return AllRecruitersResponseDto.builder()
                .id(recruiter.getId())
                .userId(user != null ? user.getId() : null)
                .firstName(user != null ? user.getFirstName() : null)
                .lastName(user != null ? user.getLastName() : null)
                .email(user != null ? user.getEmail() : null)
                .mobile(user != null ? user.getMobile() : null)
                .companyName(recruiter.getCompanyName())
                .designation(recruiter.getDesignation())
                .location(recruiter.getLocation())
                .profileImage(recruiter.getProfilePhotoUrl())
                .companyLogo(recruiter.getCompanyLogoUrl())
                .website(recruiter.getCompanyWebsite())
                .bio(recruiter.getCompanyDescription())
                .isVerified(user != null ? user.getIsVerified() : false)
                .accountStatus(user != null && user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
                .isActive(user != null ? user.getIsActive() : false)
                .isOnboardingComplete(user != null ? user.getIsOnboardingComplete() : false)
                .totalJobsPosted(user != null ? jobRepository.countByRecruiter(user) : 0L)
                .createdAt(recruiter.getCreatedAt())
                .updatedAt(recruiter.getUpdatedAt())
                .lastLoginAt(user != null ? user.getLastLogin() : null)
                .build();
    }

    private AllArtistsResponseDto mapToArtistDto(ArtistProfile artist) {
        User user = artist.getUser();

        return AllArtistsResponseDto.builder()
                .id(artist.getId())
                .userId(user != null ? user.getId() : null)
                .firstName(artist.getFirstName())
                .lastName(artist.getLastName())
                .email(user != null ? user.getEmail() : null)
                .mobile(user != null ? user.getMobile() : null)
                .stageName(artist.getStageName())
                .artistTypeName(artist.getArtistType() != null ? artist.getArtistType().getName() : null)
                .artistTypeId(artist.getArtistType() != null ? artist.getArtistType().getId() : null)
                .gender(artist.getGender() != null ? artist.getGender().name() : null)
                .dateOfBirth(artist.getDateOfBirth() != null ? artist.getDateOfBirth().toString() : null)
                .location(artist.getLocation())
                .bio(artist.getBio())
                .profileImage(artist.getProfileUrl())
                .isVerified(user != null ? user.getIsVerified() : false)
                .accountStatus(user != null && user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
                .isActive(user != null ? user.getIsActive() : false)
                .isOnboardingComplete(user != null ? user.getIsOnboardingComplete() : false)
                .totalApplications(jobApplicationRepository.countByArtistId(artist.getId()))
                .profileViews(artist.getTotalApplications() != null ? artist.getTotalApplications().longValue() : 0L)
                .createdAt(artist.getCreatedAt())
                .updatedAt(artist.getUpdatedAt())
                .lastLoginAt(user != null ? user.getLastLogin() : null)
                .build();
    }
}
