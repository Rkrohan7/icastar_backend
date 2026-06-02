package com.icastar.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icastar.platform.dto.superadmin.*;
import com.icastar.platform.entity.*;
import com.icastar.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final CastingCallApplicationRepository castingCallApplicationRepository;
    private final ArtistTypeRepository artistTypeRepository;
    private final ReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    // ==================== ADMIN USER MANAGEMENT ====================

    /**
     * Get all admin users with pagination
     */
    @Transactional(readOnly = true)
    public Page<AdminUserDto> getAllAdmins(Pageable pageable, String search, String status) {
        log.info("Fetching all admins - search: {}, status: {}", search, status);

        List<User> admins = userRepository.findByRole(User.UserRole.ADMIN);

        // Apply search filter
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            admins = admins.stream()
                    .filter(u -> (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(searchLower)) ||
                                 (u.getLastName() != null && u.getLastName().toLowerCase().contains(searchLower)) ||
                                 (u.getEmail() != null && u.getEmail().toLowerCase().contains(searchLower)))
                    .collect(Collectors.toList());
        }

        // Apply status filter
        if (status != null && !status.isEmpty()) {
            try {
                User.AccountStatus accountStatus = User.AccountStatus.valueOf(status.toUpperCase());
                admins = admins.stream()
                        .filter(u -> u.getAccountStatus() == accountStatus)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid account status: {}", status);
            }
        }

        // Convert to DTOs
        List<AdminUserDto> adminDtos = admins.stream()
                .map(this::mapToAdminDto)
                .collect(Collectors.toList());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), adminDtos.size());
        List<AdminUserDto> pagedAdmins = start < adminDtos.size() ? adminDtos.subList(start, end) : Collections.emptyList();

        return new PageImpl<>(pagedAdmins, pageable, adminDtos.size());
    }

    /**
     * Get admin by ID
     */
    @Transactional(readOnly = true)
    public AdminUserDto getAdminById(Long id) {
        log.info("Fetching admin with id: {}", id);

        User admin = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + id));

        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }

        return mapToAdminDto(admin);
    }

    /**
     * Create new admin user
     */
    @Transactional
    public AdminUserDto createAdmin(AdminUserDto.CreateAdminRequest request) {
        log.info("Creating new admin with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        // Check if mobile already exists
        if (userRepository.findByMobile(request.getMobile()).isPresent()) {
            throw new RuntimeException("Mobile already exists: " + request.getMobile());
        }

        User admin = new User();
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setEmail(request.getEmail());
        admin.setMobile(request.getMobile());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRole(User.UserRole.ADMIN);
        admin.setStatus(User.UserStatus.ACTIVE);
        admin.setAccountStatus(User.AccountStatus.ACTIVE);
        admin.setIsVerified(true);
        admin.setIsOnboardingComplete(true);
        admin.setFailedLoginAttempts(0);
        admin.setLoginAttempts(0);

        User savedAdmin = userRepository.save(admin);
        log.info("Admin created successfully with id: {}", savedAdmin.getId());

        return mapToAdminDto(savedAdmin);
    }

    /**
     * Update admin user
     */
    @Transactional
    public AdminUserDto updateAdmin(Long id, AdminUserDto.UpdateAdminRequest request) {
        log.info("Updating admin with id: {}", id);

        User admin = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + id));

        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }

        // Update fields if provided
        if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
            admin.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isEmpty()) {
            admin.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            // Check if email is different and already exists
            if (!admin.getEmail().equals(request.getEmail()) &&
                userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("Email already exists: " + request.getEmail());
            }
            admin.setEmail(request.getEmail());
        }
        if (request.getMobile() != null && !request.getMobile().isEmpty()) {
            // Check if mobile is different and already exists
            if (!admin.getMobile().equals(request.getMobile()) &&
                userRepository.findByMobile(request.getMobile()).isPresent()) {
                throw new RuntimeException("Mobile already exists: " + request.getMobile());
            }
            admin.setMobile(request.getMobile());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User savedAdmin = userRepository.save(admin);
        log.info("Admin updated successfully with id: {}", savedAdmin.getId());

        return mapToAdminDto(savedAdmin);
    }

    /**
     * Change admin status
     */
    @Transactional
    public AdminUserDto changeAdminStatus(Long id, AdminUserDto.ChangeStatusRequest request) {
        log.info("Changing status of admin with id: {} to {}", id, request.getStatus());

        User admin = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + id));

        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }

        try {
            User.AccountStatus newStatus = User.AccountStatus.valueOf(request.getStatus().toUpperCase());
            admin.setAccountStatus(newStatus);

            // Also update UserStatus for consistency
            switch (newStatus) {
                case ACTIVE:
                    admin.setStatus(User.UserStatus.ACTIVE);
                    break;
                case INACTIVE:
                    admin.setStatus(User.UserStatus.INACTIVE);
                    break;
                case SUSPENDED:
                    admin.setStatus(User.UserStatus.SUSPENDED);
                    break;
                case BANNED:
                    admin.setStatus(User.UserStatus.BANNED);
                    break;
                default:
                    break;
            }

            if (request.getReason() != null && !request.getReason().isEmpty()) {
                admin.setDeactivationReason(request.getReason());
            }

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + request.getStatus());
        }

        User savedAdmin = userRepository.save(admin);
        log.info("Admin status changed successfully to {}", savedAdmin.getAccountStatus());

        return mapToAdminDto(savedAdmin);
    }

    /**
     * Delete admin user
     */
    @Transactional
    public void deleteAdmin(Long id) {
        log.info("Deleting admin with id: {}", id);

        User admin = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + id));

        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }

        userRepository.delete(admin);
        log.info("Admin deleted successfully with id: {}", id);
    }

    /**
     * Map User entity to AdminUserDto
     */
    private AdminUserDto mapToAdminDto(User user) {
        return AdminUserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .accountStatus(user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
                .isVerified(user.getIsVerified())
                .isActive(user.getStatus() == User.UserStatus.ACTIVE)
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

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

    // ==================== AUDITION MANAGEMENT ====================

    @Transactional(readOnly = true)
    public Page<SuperAdminAuditionDto> getAllAuditions(Pageable pageable, String status, String type) {
        log.info("Fetching all auditions - status: {}, type: {}", status, type);

        Page<Audition> auditions = auditionRepository.findAll(pageable);
        return auditions.map(this::mapToAuditionDto);
    }

    @Transactional(readOnly = true)
    public SuperAdminAuditionDto getAuditionById(Long id) {
        Audition audition = auditionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audition not found with id: " + id));
        return mapToAuditionDto(audition);
    }

    @Transactional
    public SuperAdminAuditionDto updateAuditionStatus(Long id, SuperAdminAuditionDto.UpdateStatusRequest request) {
        Audition audition = auditionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audition not found with id: " + id));

        if (request.getStatus() != null) {
            audition.setStatus(Audition.AuditionStatus.valueOf(request.getStatus().toUpperCase()));
        }
        if (request.getFeedback() != null) {
            audition.setFeedback(request.getFeedback());
        }
        if (request.getRating() != null) {
            audition.setRating(request.getRating());
        }

        Audition saved = auditionRepository.save(audition);
        return mapToAuditionDto(saved);
    }

    private SuperAdminAuditionDto mapToAuditionDto(Audition audition) {
        ArtistProfile artist = audition.getArtist();
        RecruiterProfile recruiter = audition.getRecruiter();
        JobApplication jobApp = audition.getJobApplication();

        return SuperAdminAuditionDto.builder()
                .id(audition.getId())
                .title(audition.getTitle())
                .description(audition.getDescription())
                .auditionType(audition.getAuditionType() != null ? audition.getAuditionType().name() : null)
                .status(audition.getStatus() != null ? audition.getStatus().name() : null)
                .scheduledAt(audition.getScheduledAt())
                .durationMinutes(audition.getDurationMinutes())
                .meetingLink(audition.getMeetingLink())
                .instructions(audition.getInstructions())
                .feedback(audition.getFeedback())
                .rating(audition.getRating())
                .recordingUrl(audition.getRecordingUrl())
                .isOpenAudition(audition.getIsOpenAudition())
                .artistId(artist != null ? artist.getId() : null)
                .artistName(artist != null ? artist.getFirstName() + " " + artist.getLastName() : null)
                .artistEmail(artist != null && artist.getUser() != null ? artist.getUser().getEmail() : null)
                .artistProfileImage(artist != null ? artist.getProfileUrl() : null)
                .artistType(artist != null && artist.getArtistType() != null ? artist.getArtistType().getName() : null)
                .recruiterId(recruiter != null ? recruiter.getId() : null)
                .recruiterName(recruiter != null && recruiter.getUser() != null ?
                    recruiter.getUser().getFirstName() + " " + recruiter.getUser().getLastName() : null)
                .recruiterEmail(recruiter != null && recruiter.getUser() != null ? recruiter.getUser().getEmail() : null)
                .companyName(recruiter != null ? recruiter.getCompanyName() : null)
                .jobApplicationId(jobApp != null ? jobApp.getId() : null)
                .jobTitle(jobApp != null && jobApp.getJob() != null ? jobApp.getJob().getTitle() : null)
                .targetArtistTypeId(audition.getTargetArtistType() != null ? audition.getTargetArtistType().getId() : null)
                .targetArtistTypeName(audition.getTargetArtistType() != null ? audition.getTargetArtistType().getName() : null)
                .completedAt(audition.getCompletedAt())
                .createdAt(audition.getCreatedAt())
                .updatedAt(audition.getUpdatedAt())
                .build();
    }

    // ==================== JOB APPROVAL MANAGEMENT ====================

    @Transactional(readOnly = true)
    public Page<JobApprovalDto> getJobsForApproval(Pageable pageable) {
        Page<Job> jobs = jobRepository.findByStatus(Job.JobStatus.PENDING_APPROVAL, pageable);
        return jobs.map(this::mapToJobApprovalDto);
    }

    @Transactional
    public JobApprovalDto approveJob(Long jobId, User approver) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));

        job.setStatus(Job.JobStatus.ACTIVE);
        job.setApprovedAt(LocalDateTime.now());
        job.setApprovedBy(approver);
        job.setPublishedAt(LocalDateTime.now());

        Job saved = jobRepository.save(job);
        return mapToJobApprovalDto(saved);
    }

    @Transactional
    public JobApprovalDto rejectJob(Long jobId, User rejector, String reason) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));

        job.setStatus(Job.JobStatus.REJECTED);
        job.setRejectedAt(LocalDateTime.now());
        job.setRejectedBy(rejector);
        job.setRejectionReason(reason);

        Job saved = jobRepository.save(job);
        return mapToJobApprovalDto(saved);
    }

    private JobApprovalDto mapToJobApprovalDto(Job job) {
        User recruiter = job.getRecruiter();
        return JobApprovalDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .location(job.getLocation())
                .jobType(job.getJobType() != null ? job.getJobType().name() : null)
                .experienceLevel(job.getExperienceLevel() != null ? job.getExperienceLevel().name() : null)
                .budgetMin(job.getBudgetMin())
                .budgetMax(job.getBudgetMax())
                .currency(job.getCurrency())
                .durationDays(job.getDurationDays())
                .startDate(job.getStartDate())
                .endDate(job.getEndDate())
                .applicationDeadline(job.getApplicationDeadline())
                .isRemote(job.getIsRemote())
                .isUrgent(job.getIsUrgent())
                .isFeatured(job.getIsFeatured())
                .status(job.getStatus() != null ? job.getStatus().name() : null)
                .tags(job.getTags())
                .skillsRequired(job.getSkillsRequired())
                .benefits(job.getBenefits())
                .contactEmail(job.getContactEmail())
                .contactPhone(job.getContactPhone())
                .recruiterId(recruiter != null ? recruiter.getId() : null)
                .recruiterName(recruiter != null ? recruiter.getFirstName() + " " + recruiter.getLastName() : null)
                .recruiterEmail(recruiter != null ? recruiter.getEmail() : null)
                .approvedAt(job.getApprovedAt())
                .approvedByName(job.getApprovedBy() != null ?
                    job.getApprovedBy().getFirstName() + " " + job.getApprovedBy().getLastName() : null)
                .rejectedAt(job.getRejectedAt())
                .rejectedByName(job.getRejectedBy() != null ?
                    job.getRejectedBy().getFirstName() + " " + job.getRejectedBy().getLastName() : null)
                .rejectionReason(job.getRejectionReason())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    // ==================== JOB APPLICATIONS ====================

    @Transactional(readOnly = true)
    public Page<SuperAdminJobApplicationDto> getAllJobApplications(Pageable pageable, String status) {
        Page<JobApplication> applications;
        if (status != null && !status.isEmpty()) {
            try {
                JobApplication.ApplicationStatus appStatus = JobApplication.ApplicationStatus.valueOf(status.toUpperCase());
                applications = jobApplicationRepository.findByStatus(appStatus, pageable);
            } catch (IllegalArgumentException e) {
                applications = jobApplicationRepository.findAll(pageable);
            }
        } else {
            applications = jobApplicationRepository.findAll(pageable);
        }
        return applications.map(this::mapToJobApplicationDto);
    }

    @Transactional(readOnly = true)
    public SuperAdminJobApplicationDto getJobApplicationById(Long id) {
        JobApplication app = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job application not found with id: " + id));
        return mapToJobApplicationDto(app);
    }

    private SuperAdminJobApplicationDto mapToJobApplicationDto(JobApplication app) {
        Job job = app.getJob();
        ArtistProfile artist = app.getArtist();
        User recruiter = job != null ? job.getRecruiter() : null;

        return SuperAdminJobApplicationDto.builder()
                .id(app.getId())
                .status(app.getStatus() != null ? app.getStatus().name() : null)
                .coverLetter(app.getCoverLetter())
                .expectedSalary(app.getExpectedSalary())
                .availabilityDate(app.getAvailabilityDate())
                .portfolioUrl(app.getPortfolioUrl())
                .resumeUrl(app.getResumeUrl())
                .demoReelUrl(app.getDemoReelUrl())
                .appliedAt(app.getAppliedAt())
                .reviewedAt(app.getReviewedAt())
                .interviewScheduledAt(app.getInterviewScheduledAt())
                .interviewNotes(app.getInterviewNotes())
                .rejectionReason(app.getRejectionReason())
                .feedback(app.getFeedback())
                .rating(app.getRating())
                .isShortlisted(app.getIsShortlisted())
                .isHired(app.getIsHired())
                .hiredAt(app.getHiredAt())
                .offeredSalary(app.getOfferedSalary())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .jobLocation(job != null ? job.getLocation() : null)
                .jobType(job != null && job.getJobType() != null ? job.getJobType().name() : null)
                .jobStatus(job != null && job.getStatus() != null ? job.getStatus().name() : null)
                .artistId(artist != null ? artist.getId() : null)
                .artistName(artist != null ? artist.getFirstName() + " " + artist.getLastName() : null)
                .artistEmail(artist != null && artist.getUser() != null ? artist.getUser().getEmail() : null)
                .artistMobile(artist != null && artist.getUser() != null ? artist.getUser().getMobile() : null)
                .artistProfileImage(artist != null ? artist.getProfileUrl() : null)
                .artistType(artist != null && artist.getArtistType() != null ? artist.getArtistType().getName() : null)
                .recruiterId(recruiter != null ? recruiter.getId() : null)
                .recruiterName(recruiter != null ? recruiter.getFirstName() + " " + recruiter.getLastName() : null)
                .recruiterEmail(recruiter != null ? recruiter.getEmail() : null)
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    // ==================== AUDITION APPLICATIONS (Casting Call) ====================

    @Transactional(readOnly = true)
    public Page<SuperAdminJobApplicationDto> getAllAuditionApplications(Pageable pageable, String status) {
        Page<CastingCallApplication> applications;
        if (status != null && !status.isEmpty()) {
            try {
                CastingCallApplication.ApplicationStatus appStatus =
                    CastingCallApplication.ApplicationStatus.valueOf(status.toUpperCase());
                applications = castingCallApplicationRepository.findByStatus(appStatus, pageable);
            } catch (IllegalArgumentException e) {
                applications = castingCallApplicationRepository.findAll(pageable);
            }
        } else {
            applications = castingCallApplicationRepository.findAll(pageable);
        }
        return applications.map(this::mapToCastingCallApplicationDto);
    }

    private SuperAdminJobApplicationDto mapToCastingCallApplicationDto(CastingCallApplication app) {
        CastingCall castingCall = app.getCastingCall();
        ArtistProfile artist = app.getArtist();
        RecruiterProfile recruiter = castingCall != null ? castingCall.getRecruiter() : null;

        return SuperAdminJobApplicationDto.builder()
                .id(app.getId())
                .status(app.getStatus() != null ? app.getStatus().name() : null)
                .coverLetter(app.getCoverLetter())
                .portfolioUrl(app.getPortfolioUrl())
                .resumeUrl(app.getResumeUrl())
                .demoReelUrl(app.getDemoReelUrl())
                .appliedAt(app.getAppliedAt())
                .reviewedAt(app.getReviewedAt())
                .rejectionReason(app.getRejectionReason())
                .feedback(app.getFeedback())
                .rating(app.getRating())
                .isShortlisted(app.getIsShortlisted())
                .jobId(castingCall != null ? castingCall.getId() : null)
                .jobTitle(castingCall != null ? castingCall.getTitle() : null)
                .jobLocation(castingCall != null ? castingCall.getLocation() : null)
                .artistId(artist != null ? artist.getId() : null)
                .artistName(artist != null ? artist.getFirstName() + " " + artist.getLastName() : null)
                .artistEmail(artist != null && artist.getUser() != null ? artist.getUser().getEmail() : null)
                .artistMobile(artist != null && artist.getUser() != null ? artist.getUser().getMobile() : null)
                .artistProfileImage(artist != null ? artist.getProfileUrl() : null)
                .artistType(artist != null && artist.getArtistType() != null ? artist.getArtistType().getName() : null)
                .recruiterId(recruiter != null ? recruiter.getId() : null)
                .recruiterName(recruiter != null && recruiter.getUser() != null ?
                    recruiter.getUser().getFirstName() + " " + recruiter.getUser().getLastName() : null)
                .recruiterEmail(recruiter != null && recruiter.getUser() != null ? recruiter.getUser().getEmail() : null)
                .companyName(recruiter != null ? recruiter.getCompanyName() : null)
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    // ==================== INTERVIEWS ====================

    @Transactional(readOnly = true)
    public Page<SuperAdminInterviewDto> getAllInterviews(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        List<JobApplication> interviews = jobApplicationRepository.findUpcomingInterviews(now);

        List<SuperAdminInterviewDto> interviewDtos = interviews.stream()
                .map(this::mapToInterviewDto)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), interviewDtos.size());
        List<SuperAdminInterviewDto> pagedInterviews = start < interviewDtos.size() ?
            interviewDtos.subList(start, end) : Collections.emptyList();

        return new PageImpl<>(pagedInterviews, pageable, interviewDtos.size());
    }

    private SuperAdminInterviewDto mapToInterviewDto(JobApplication app) {
        Job job = app.getJob();
        ArtistProfile artist = app.getArtist();
        User recruiter = job != null ? job.getRecruiter() : null;

        return SuperAdminInterviewDto.builder()
                .applicationId(app.getId())
                .interviewScheduledAt(app.getInterviewScheduledAt())
                .interviewNotes(app.getInterviewNotes())
                .status(app.getStatus() != null ? app.getStatus().name() : null)
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .jobLocation(job != null ? job.getLocation() : null)
                .artistId(artist != null ? artist.getId() : null)
                .artistName(artist != null ? artist.getFirstName() + " " + artist.getLastName() : null)
                .artistEmail(artist != null && artist.getUser() != null ? artist.getUser().getEmail() : null)
                .artistMobile(artist != null && artist.getUser() != null ? artist.getUser().getMobile() : null)
                .artistProfileImage(artist != null ? artist.getProfileUrl() : null)
                .recruiterId(recruiter != null ? recruiter.getId() : null)
                .recruiterName(recruiter != null ? recruiter.getFirstName() + " " + recruiter.getLastName() : null)
                .recruiterEmail(recruiter != null ? recruiter.getEmail() : null)
                .createdAt(app.getCreatedAt())
                .build();
    }

    // ==================== ARTIST PORTFOLIO ====================

    @Transactional(readOnly = true)
    public SuperAdminArtistPortfolioDto getArtistPortfolio(Long artistId) {
        ArtistProfile artist = artistProfileRepository.findById(artistId)
                .orElseThrow(() -> new RuntimeException("Artist not found with id: " + artistId));
        return mapToArtistPortfolioDto(artist);
    }

    private SuperAdminArtistPortfolioDto mapToArtistPortfolioDto(ArtistProfile artist) {
        User user = artist.getUser();

        List<String> skills = parseJsonArray(artist.getSkills());
        List<String> languages = parseJsonArray(artist.getLanguagesSpoken());
        List<String> comfortableAreas = parseJsonArray(artist.getComfortableAreas());
        List<String> portfolioUrls = parseJsonArray(artist.getPortfolioUrls());
        List<String> travelCities = parseJsonArray(artist.getTravelCities());
        List<SuperAdminArtistPortfolioDto.ProjectDto> projects = parseProjects(artist.getProjectsWorked());

        return SuperAdminArtistPortfolioDto.builder()
                .id(artist.getId())
                .userId(user != null ? user.getId() : null)
                .firstName(artist.getFirstName())
                .lastName(artist.getLastName())
                .stageName(artist.getStageName())
                .email(user != null ? user.getEmail() : null)
                .mobile(user != null ? user.getMobile() : null)
                .bio(artist.getBio())
                .dateOfBirth(artist.getDateOfBirth())
                .gender(artist.getGender() != null ? artist.getGender().name() : null)
                .location(artist.getLocation())
                .maritalStatus(artist.getMaritalStatus() != null ? artist.getMaritalStatus().name() : null)
                .artistTypeId(artist.getArtistType() != null ? artist.getArtistType().getId() : null)
                .artistTypeName(artist.getArtistType() != null ? artist.getArtistType().getName() : null)
                .weight(artist.getWeight())
                .height(artist.getHeight())
                .hairColor(artist.getHairColor())
                .hairLength(artist.getHairLength())
                .hasTattoo(artist.getHasTattoo())
                .hasMole(artist.getHasMole())
                .shoeSize(artist.getShoeSize())
                .eyeColor(artist.getEyeColor())
                .complexion(artist.getComplexion())
                .experienceYears(artist.getExperienceYears())
                .skills(skills)
                .languagesSpoken(languages)
                .comfortableAreas(comfortableAreas)
                .profileUrl(artist.getProfileUrl())
                .coverPhotoUrl(artist.getCoverPhotoUrl())
                .photoUrl(artist.getPhotoUrl())
                .videoUrl(artist.getVideoUrl())
                .danceShowreelUrl(artist.getDanceShowreelUrl())
                .portfolioUrls(portfolioUrls)
                .projectsWorked(projects)
                .idProofUrl(artist.getIdProofUrl())
                .faceVerificationUrl(artist.getFaceVerificationUrl())
                .idProofVerified(artist.getIdProofVerified())
                .idProofUploadedAt(artist.getIdProofUploadedAt())
                .hasPassport(artist.getHasPassport())
                .travelCities(travelCities)
                .hourlyRate(artist.getHourlyRate())
                .isVerifiedBadge(artist.getIsVerifiedBadge())
                .verificationRequestedAt(artist.getVerificationRequestedAt())
                .verificationApprovedAt(artist.getVerificationApprovedAt())
                .totalApplications(artist.getTotalApplications())
                .successfulHires(artist.getSuccessfulHires())
                .isProfileComplete(artist.getIsProfileComplete())
                .accountStatus(user != null && user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
                .isActive(user != null ? user.getIsActive() : false)
                .isOnboardingComplete(user != null ? user.getIsOnboardingComplete() : false)
                .lastLoginAt(user != null ? user.getLastLogin() : null)
                .createdAt(artist.getCreatedAt())
                .updatedAt(artist.getUpdatedAt())
                .build();
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<SuperAdminArtistPortfolioDto.ProjectDto> parseProjects(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<SuperAdminArtistPortfolioDto.ProjectDto>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ==================== REPORT CONTENT ====================

    @Transactional(readOnly = true)
    public Page<SuperAdminReportContentDto> getAllReports(Pageable pageable, String status, String priority) {
        Page<Report> reports;
        if (status != null && !status.isEmpty()) {
            try {
                Report.ReportStatus reportStatus = Report.ReportStatus.valueOf(status.toUpperCase());
                if (priority != null && !priority.isEmpty()) {
                    Report.ReportPriority reportPriority = Report.ReportPriority.valueOf(priority.toUpperCase());
                    reports = reportRepository.findByStatusAndPriority(reportStatus, reportPriority, pageable);
                } else {
                    reports = reportRepository.findByStatus(reportStatus, pageable);
                }
            } catch (IllegalArgumentException e) {
                reports = reportRepository.findAll(pageable);
            }
        } else {
            reports = reportRepository.findAll(pageable);
        }
        return reports.map(this::mapToReportDto);
    }

    @Transactional(readOnly = true)
    public SuperAdminReportContentDto getReportById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + id));
        return mapToReportDto(report);
    }

    @Transactional
    public SuperAdminReportContentDto reviewReport(Long id, SuperAdminReportContentDto.ReviewReportRequest request, User reviewer) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + id));

        if (request.getStatus() != null) {
            report.setStatus(Report.ReportStatus.valueOf(request.getStatus().toUpperCase()));
        }
        if (request.getPriority() != null) {
            report.setPriority(Report.ReportPriority.valueOf(request.getPriority().toUpperCase()));
        }
        if (request.getResolutionNotes() != null) {
            report.setResolutionNotes(request.getResolutionNotes());
        }
        if (request.getActionTaken() != null) {
            report.setActionTaken(Report.ActionTaken.valueOf(request.getActionTaken().toUpperCase()));
        }
        report.setReviewedBy(reviewer);
        report.setReviewedAt(LocalDateTime.now());

        Report saved = reportRepository.save(report);
        return mapToReportDto(saved);
    }

    private SuperAdminReportContentDto mapToReportDto(Report report) {
        User reporter = report.getReporter();
        User reportedUser = report.getReportedUser();
        User reviewer = report.getReviewedBy();

        List<String> evidenceUrls = parseJsonArray(report.getEvidenceUrls());

        return SuperAdminReportContentDto.builder()
                .id(report.getId())
                .reportType(report.getReportType() != null ? report.getReportType().name() : null)
                .entityType(report.getEntityType())
                .entityId(report.getEntityId())
                .reason(report.getReason() != null ? report.getReason().name() : null)
                .description(report.getDescription())
                .evidenceUrls(evidenceUrls)
                .status(report.getStatus() != null ? report.getStatus().name() : null)
                .priority(report.getPriority() != null ? report.getPriority().name() : null)
                .resolutionNotes(report.getResolutionNotes())
                .actionTaken(report.getActionTaken() != null ? report.getActionTaken().name() : null)
                .reporterId(reporter != null ? reporter.getId() : null)
                .reporterName(reporter != null ? reporter.getFirstName() + " " + reporter.getLastName() : null)
                .reporterEmail(reporter != null ? reporter.getEmail() : null)
                .reportedUserId(reportedUser != null ? reportedUser.getId() : null)
                .reportedUserName(reportedUser != null ? reportedUser.getFirstName() + " " + reportedUser.getLastName() : null)
                .reportedUserEmail(reportedUser != null ? reportedUser.getEmail() : null)
                .reportedUserRole(reportedUser != null && reportedUser.getRole() != null ? reportedUser.getRole().name() : null)
                .reviewedById(reviewer != null ? reviewer.getId() : null)
                .reviewedByName(reviewer != null ? reviewer.getFirstName() + " " + reviewer.getLastName() : null)
                .reviewedAt(report.getReviewedAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    // ==================== CATEGORIES (ARTIST TYPES) ====================

    @Transactional(readOnly = true)
    public Page<SuperAdminCategoryDto> getAllCategories(Pageable pageable) {
        Page<ArtistType> categories = artistTypeRepository.findAll(pageable);
        return categories.map(this::mapToCategoryDto);
    }

    @Transactional(readOnly = true)
    public SuperAdminCategoryDto getCategoryById(Long id) {
        ArtistType category = artistTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        return mapToCategoryDto(category);
    }

    @Transactional
    public SuperAdminCategoryDto createCategory(SuperAdminCategoryDto.CreateCategoryRequest request) {
        ArtistType category = new ArtistType();
        category.setName(request.getName());
        category.setDisplayName(request.getDisplayName());
        category.setDescription(request.getDescription());
        category.setIconUrl(request.getIconUrl());
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        ArtistType saved = artistTypeRepository.save(category);
        return mapToCategoryDto(saved);
    }

    @Transactional
    public SuperAdminCategoryDto updateCategory(Long id, SuperAdminCategoryDto.UpdateCategoryRequest request) {
        ArtistType category = artistTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        if (request.getName() != null) category.setName(request.getName());
        if (request.getDisplayName() != null) category.setDisplayName(request.getDisplayName());
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getIconUrl() != null) category.setIconUrl(request.getIconUrl());
        if (request.getIsActive() != null) category.setIsActive(request.getIsActive());
        if (request.getSortOrder() != null) category.setSortOrder(request.getSortOrder());

        ArtistType saved = artistTypeRepository.save(category);
        return mapToCategoryDto(saved);
    }

    @Transactional
    public void deleteCategory(Long id) {
        ArtistType category = artistTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        artistTypeRepository.delete(category);
    }

    private SuperAdminCategoryDto mapToCategoryDto(ArtistType category) {
        Long artistCount = artistProfileRepository.countByArtistTypeId(category.getId());

        List<SuperAdminCategoryDto.FieldDto> fields = category.getFields() != null ?
            category.getFields().stream()
                .map(f -> SuperAdminCategoryDto.FieldDto.builder()
                    .id(f.getId())
                    .fieldName(f.getFieldName())
                    .fieldLabel(f.getFieldLabel())
                    .fieldType(f.getFieldType() != null ? f.getFieldType().name() : null)
                    .isRequired(f.getIsRequired())
                    .options(f.getOptions())
                    .sortOrder(f.getSortOrder())
                    .build())
                .collect(Collectors.toList()) : Collections.emptyList();

        return SuperAdminCategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .displayName(category.getDisplayName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .isActive(category.getIsActive())
                .sortOrder(category.getSortOrder())
                .artistCount(artistCount)
                .fields(fields)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    // ==================== SKILLS ====================

    @Transactional(readOnly = true)
    public List<SuperAdminSkillDto> getAllSkills() {
        List<ArtistProfile> artists = artistProfileRepository.findAll();
        List<Job> jobs = jobRepository.findAll();

        Map<String, Long> artistSkillCount = new HashMap<>();
        Map<String, Long> jobSkillCount = new HashMap<>();

        // Count skills from artists
        for (ArtistProfile artist : artists) {
            List<String> skills = parseJsonArray(artist.getSkills());
            for (String skill : skills) {
                String normalizedSkill = skill.trim().toLowerCase();
                artistSkillCount.merge(normalizedSkill, 1L, Long::sum);
            }
        }

        // Count skills from jobs
        for (Job job : jobs) {
            List<String> skills = parseJsonArray(job.getSkillsRequired());
            for (String skill : skills) {
                String normalizedSkill = skill.trim().toLowerCase();
                jobSkillCount.merge(normalizedSkill, 1L, Long::sum);
            }
        }

        // Merge and create DTOs
        Set<String> allSkills = new HashSet<>();
        allSkills.addAll(artistSkillCount.keySet());
        allSkills.addAll(jobSkillCount.keySet());

        return allSkills.stream()
                .map(skill -> SuperAdminSkillDto.builder()
                        .name(skill)
                        .artistCount(artistSkillCount.getOrDefault(skill, 0L))
                        .jobCount(jobSkillCount.getOrDefault(skill, 0L))
                        .build())
                .sorted((a, b) -> Long.compare(b.getArtistCount() + b.getJobCount(), a.getArtistCount() + a.getJobCount()))
                .collect(Collectors.toList());
    }
}
