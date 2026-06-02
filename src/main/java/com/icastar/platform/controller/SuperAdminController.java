package com.icastar.platform.controller;

import com.icastar.platform.dto.superadmin.*;
import com.icastar.platform.entity.Job;
import com.icastar.platform.entity.User;
import com.icastar.platform.service.SuperAdminService;
import com.icastar.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/super-admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Super Admin Dashboard, Reports & Management APIs")
@PreAuthorize("hasRole('ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final UserService userService;

    // ==================== DASHBOARD APIs ====================

    @GetMapping("/dashboard")
    @Operation(summary = "Get Super Admin Dashboard", description = "Get comprehensive dashboard with all statistics")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        log.info("Super Admin Dashboard requested");

        SuperAdminDashboardDto dashboard = superAdminService.getDashboardStats();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Dashboard data retrieved successfully");
        response.put("data", dashboard);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Get Quick Summary", description = "Get quick summary of key metrics")
    public ResponseEntity<Map<String, Object>> getQuickSummary() {
        log.info("Quick summary requested");

        SuperAdminDashboardDto dashboard = superAdminService.getDashboardStats();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalUsers", dashboard.getTotalUsers());
        summary.put("totalArtists", dashboard.getTotalArtists());
        summary.put("totalRecruiters", dashboard.getTotalRecruiters());
        summary.put("totalJobs", dashboard.getTotalJobs());
        summary.put("activeJobs", dashboard.getActiveJobs());
        summary.put("totalApplications", dashboard.getTotalApplications());
        summary.put("pendingApplications", dashboard.getPendingApplications());
        summary.put("newUsersToday", dashboard.getNewUsersToday());
        summary.put("generatedAt", dashboard.getGeneratedAt());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", summary);

        return ResponseEntity.ok(response);
    }

    // ==================== ADMIN USERS APIs ====================

    @GetMapping("/admins")
    @Operation(summary = "Get All Admins", description = "Get all admin users with pagination")
    public ResponseEntity<Map<String, Object>> getAllAdmins(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Account status filter") @RequestParam(required = false) String status) {

        log.info("Fetching all admins - page: {}, size: {}, search: {}", page, size, search);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AdminUserDto> admins = superAdminService.getAllAdmins(pageable, search, status);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Admin users retrieved successfully");
        response.put("data", admins.getContent());
        response.put("currentPage", admins.getNumber());
        response.put("totalItems", admins.getTotalElements());
        response.put("totalPages", admins.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admins/{id}")
    @Operation(summary = "Get Admin Details", description = "Get detailed information of a specific admin")
    public ResponseEntity<Map<String, Object>> getAdminDetails(@PathVariable Long id) {
        log.info("Fetching admin details for id: {}", id);

        try {
            AdminUserDto admin = superAdminService.getAdminById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Admin details retrieved successfully");
            response.put("data", admin);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/admins")
    @Operation(summary = "Create Admin", description = "Create a new admin user")
    public ResponseEntity<Map<String, Object>> createAdmin(@Valid @RequestBody AdminUserDto.CreateAdminRequest request) {
        log.info("Creating new admin with email: {}", request.getEmail());

        try {
            AdminUserDto admin = superAdminService.createAdmin(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Admin created successfully");
            response.put("data", admin);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/admins/{id}")
    @Operation(summary = "Update Admin", description = "Update an existing admin user")
    public ResponseEntity<Map<String, Object>> updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserDto.UpdateAdminRequest request) {
        log.info("Updating admin with id: {}", id);

        try {
            AdminUserDto admin = superAdminService.updateAdmin(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Admin updated successfully");
            response.put("data", admin);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/admins/{id}/status")
    @Operation(summary = "Change Admin Status", description = "Change the status of an admin user")
    public ResponseEntity<Map<String, Object>> changeAdminStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserDto.ChangeStatusRequest request) {
        log.info("Changing status of admin with id: {} to {}", id, request.getStatus());

        try {
            AdminUserDto admin = superAdminService.changeAdminStatus(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Admin status changed successfully");
            response.put("data", admin);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/admins/{id}")
    @Operation(summary = "Delete Admin", description = "Delete an admin user")
    public ResponseEntity<Map<String, Object>> deleteAdmin(@PathVariable Long id) {
        log.info("Deleting admin with id: {}", id);

        try {
            superAdminService.deleteAdmin(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Admin deleted successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== ALL RECRUITERS APIs ====================

    @GetMapping("/recruiters")
    @Operation(summary = "Get All Recruiters", description = "Get all recruiters with detailed information")
    public ResponseEntity<Map<String, Object>> getAllRecruiters(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Account status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Recruiter category") @RequestParam(required = false) String category) {

        log.info("Fetching all recruiters - page: {}, size: {}, search: {}", page, size, search);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AllRecruitersResponseDto> recruiters = superAdminService.getAllRecruiters(pageable, search, status, category);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Recruiters retrieved successfully");
        response.put("data", recruiters.getContent());
        response.put("currentPage", recruiters.getNumber());
        response.put("totalItems", recruiters.getTotalElements());
        response.put("totalPages", recruiters.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recruiters/{id}")
    @Operation(summary = "Get Recruiter Details", description = "Get detailed information of a specific recruiter")
    public ResponseEntity<Map<String, Object>> getRecruiterDetails(@PathVariable Long id) {
        log.info("Fetching recruiter details for id: {}", id);

        // TODO: Implement individual recruiter fetch
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Recruiter details retrieved successfully");

        return ResponseEntity.ok(response);
    }

    // ==================== ALL ARTISTS APIs ====================

    @GetMapping("/artists")
    @Operation(summary = "Get All Artists", description = "Get all artists with detailed information")
    public ResponseEntity<Map<String, Object>> getAllArtists(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Account status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Artist type filter") @RequestParam(required = false) String artistType) {

        log.info("Fetching all artists - page: {}, size: {}, search: {}", page, size, search);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AllArtistsResponseDto> artists = superAdminService.getAllArtists(pageable, search, status, artistType);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Artists retrieved successfully");
        response.put("data", artists.getContent());
        response.put("currentPage", artists.getNumber());
        response.put("totalItems", artists.getTotalElements());
        response.put("totalPages", artists.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/artists/{id}")
    @Operation(summary = "Get Artist Details", description = "Get detailed information of a specific artist")
    public ResponseEntity<Map<String, Object>> getArtistDetails(@PathVariable Long id) {
        log.info("Fetching artist details for id: {}", id);

        // TODO: Implement individual artist fetch
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Artist details retrieved successfully");

        return ResponseEntity.ok(response);
    }

    // ==================== ALL JOBS APIs ====================

    @GetMapping("/jobs")
    @Operation(summary = "Get All Jobs", description = "Get all jobs with detailed information")
    public ResponseEntity<Map<String, Object>> getAllJobs(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Job status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Job type filter") @RequestParam(required = false) String jobType) {

        log.info("Fetching all jobs - page: {}, size: {}, search: {}", page, size, search);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Job> jobs = superAdminService.getAllJobs(pageable, search, status, jobType);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Jobs retrieved successfully");
        response.put("data", jobs.getContent());
        response.put("currentPage", jobs.getNumber());
        response.put("totalItems", jobs.getTotalElements());
        response.put("totalPages", jobs.getTotalPages());

        return ResponseEntity.ok(response);
    }

    // ==================== REPORTS APIs ====================

    @GetMapping("/reports/users")
    @Operation(summary = "Get User Report", description = "Get user registration and activity report")
    public ResponseEntity<Map<String, Object>> getUserReport(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Generating user report from {} to {}", startDate, endDate);

        ReportsDto.UserReportDto report = superAdminService.getUserReport(startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User report generated successfully");
        response.put("data", report);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/jobs")
    @Operation(summary = "Get Job Report", description = "Get job posting and application report")
    public ResponseEntity<Map<String, Object>> getJobReport(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Generating job report from {} to {}", startDate, endDate);

        ReportsDto.JobReportDto report = superAdminService.getJobReport(startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Job report generated successfully");
        response.put("data", report);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/overview")
    @Operation(summary = "Get Overview Report", description = "Get platform overview report")
    public ResponseEntity<Map<String, Object>> getOverviewReport(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Generating overview report from {} to {}", startDate, endDate);

        SuperAdminDashboardDto dashboard = superAdminService.getDashboardStats();
        ReportsDto.UserReportDto userReport = superAdminService.getUserReport(startDate, endDate);
        ReportsDto.JobReportDto jobReport = superAdminService.getJobReport(startDate, endDate);

        Map<String, Object> overview = new HashMap<>();
        overview.put("dashboard", dashboard);
        overview.put("userReport", userReport);
        overview.put("jobReport", jobReport);
        overview.put("period", Map.of("startDate", startDate, "endDate", endDate));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Overview report generated successfully");
        response.put("data", overview);

        return ResponseEntity.ok(response);
    }

    // ==================== CONFIGURATION APIs ====================

    @GetMapping("/config")
    @Operation(summary = "Get System Configuration", description = "Get current system configuration")
    public ResponseEntity<Map<String, Object>> getSystemConfig() {
        log.info("Fetching system configuration");

        // Return basic configuration (extend as needed)
        SystemConfigDto config = SystemConfigDto.builder()
                .platformName("iCastar")
                .platformEmail("support@icastar.com")
                .allowNewRegistrations(true)
                .requireEmailVerification(true)
                .requireMobileVerification(true)
                .otpExpirationMinutes(10)
                .otpLength(4)
                .maxJobsPerRecruiter(100)
                .jobExpirationDays(30)
                .emailNotificationsEnabled(true)
                .smsNotificationsEnabled(true)
                .pushNotificationsEnabled(true)
                .build();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "System configuration retrieved successfully");
        response.put("data", config);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/config")
    @Operation(summary = "Update System Configuration", description = "Update system configuration settings")
    public ResponseEntity<Map<String, Object>> updateSystemConfig(@RequestBody SystemConfigDto.UpdateConfigRequest request) {
        log.info("Updating system configuration - key: {}", request.getKey());

        // TODO: Implement actual configuration update logic

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Configuration updated successfully");

        return ResponseEntity.ok(response);
    }

    // ==================== STATISTICS APIs ====================

    @GetMapping("/stats/users")
    @Operation(summary = "Get User Statistics", description = "Get detailed user statistics")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        log.info("Fetching user statistics");

        SuperAdminDashboardDto dashboard = superAdminService.getDashboardStats();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", dashboard.getTotalUsers());
        stats.put("totalArtists", dashboard.getTotalArtists());
        stats.put("totalRecruiters", dashboard.getTotalRecruiters());
        stats.put("totalAdmins", dashboard.getTotalAdmins());
        stats.put("activeUsers", dashboard.getActiveUsers());
        stats.put("inactiveUsers", dashboard.getInactiveUsers());
        stats.put("suspendedUsers", dashboard.getSuspendedUsers());
        stats.put("bannedUsers", dashboard.getBannedUsers());
        stats.put("verifiedUsers", dashboard.getVerifiedUsers());
        stats.put("unverifiedUsers", dashboard.getUnverifiedUsers());
        stats.put("newUsersToday", dashboard.getNewUsersToday());
        stats.put("newUsersThisWeek", dashboard.getNewUsersThisWeek());
        stats.put("newUsersThisMonth", dashboard.getNewUsersThisMonth());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/jobs")
    @Operation(summary = "Get Job Statistics", description = "Get detailed job statistics")
    public ResponseEntity<Map<String, Object>> getJobStats() {
        log.info("Fetching job statistics");

        SuperAdminDashboardDto dashboard = superAdminService.getDashboardStats();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", dashboard.getTotalJobs());
        stats.put("activeJobs", dashboard.getActiveJobs());
        stats.put("closedJobs", dashboard.getClosedJobs());
        stats.put("draftJobs", dashboard.getDraftJobs());
        stats.put("featuredJobs", dashboard.getFeaturedJobs());
        stats.put("jobTypeDistribution", dashboard.getJobTypeDistribution());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/applications")
    @Operation(summary = "Get Application Statistics", description = "Get detailed application statistics")
    public ResponseEntity<Map<String, Object>> getApplicationStats() {
        log.info("Fetching application statistics");

        SuperAdminDashboardDto dashboard = superAdminService.getDashboardStats();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalApplications", dashboard.getTotalApplications());
        stats.put("pendingApplications", dashboard.getPendingApplications());
        stats.put("acceptedApplications", dashboard.getAcceptedApplications());
        stats.put("rejectedApplications", dashboard.getRejectedApplications());
        stats.put("shortlistedApplications", dashboard.getShortlistedApplications());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/distribution")
    @Operation(summary = "Get Distribution Statistics", description = "Get artist type and job type distribution")
    public ResponseEntity<Map<String, Object>> getDistributionStats() {
        log.info("Fetching distribution statistics");

        SuperAdminDashboardDto dashboard = superAdminService.getDashboardStats();

        Map<String, Object> stats = new HashMap<>();
        stats.put("artistTypeDistribution", dashboard.getArtistTypeDistribution());
        stats.put("jobTypeDistribution", dashboard.getJobTypeDistribution());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);

        return ResponseEntity.ok(response);
    }

    // ==================== AUDITION APIs ====================

    @GetMapping("/auditions")
    @Operation(summary = "Get All Auditions", description = "Get all auditions with pagination")
    public ResponseEntity<Map<String, Object>> getAllAuditions(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "Status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Type filter") @RequestParam(required = false) String type) {

        log.info("Fetching all auditions - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SuperAdminAuditionDto> auditions = superAdminService.getAllAuditions(pageable, status, type);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Auditions retrieved successfully");
        response.put("data", auditions.getContent());
        response.put("currentPage", auditions.getNumber());
        response.put("totalItems", auditions.getTotalElements());
        response.put("totalPages", auditions.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/auditions/{id}")
    @Operation(summary = "Get Audition Details", description = "Get detailed information of a specific audition")
    public ResponseEntity<Map<String, Object>> getAuditionDetails(@PathVariable Long id) {
        log.info("Fetching audition details for id: {}", id);

        try {
            SuperAdminAuditionDto audition = superAdminService.getAuditionById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Audition details retrieved successfully");
            response.put("data", audition);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/auditions/{id}/status")
    @Operation(summary = "Update Audition Status", description = "Update the status of an audition")
    public ResponseEntity<Map<String, Object>> updateAuditionStatus(
            @PathVariable Long id,
            @RequestBody SuperAdminAuditionDto.UpdateStatusRequest request) {
        log.info("Updating audition status for id: {}", id);

        try {
            SuperAdminAuditionDto audition = superAdminService.updateAuditionStatus(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Audition status updated successfully");
            response.put("data", audition);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== JOB APPROVAL APIs ====================

    @GetMapping("/jobs/pending-approval")
    @Operation(summary = "Get Jobs Pending Approval", description = "Get all jobs pending approval")
    public ResponseEntity<Map<String, Object>> getJobsPendingApproval(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("Fetching jobs pending approval - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<JobApprovalDto> jobs = superAdminService.getJobsForApproval(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Jobs pending approval retrieved successfully");
        response.put("data", jobs.getContent());
        response.put("currentPage", jobs.getNumber());
        response.put("totalItems", jobs.getTotalElements());
        response.put("totalPages", jobs.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/jobs/{id}/approve")
    @Operation(summary = "Approve Job", description = "Approve a job posting")
    public ResponseEntity<Map<String, Object>> approveJob(@PathVariable Long id, Authentication authentication) {
        log.info("Approving job with id: {}", id);

        try {
            User approver = userService.getCurrentUser(authentication);
            JobApprovalDto job = superAdminService.approveJob(id, approver);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job approved successfully");
            response.put("data", job);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/jobs/{id}/reject")
    @Operation(summary = "Reject Job", description = "Reject a job posting")
    public ResponseEntity<Map<String, Object>> rejectJob(
            @PathVariable Long id,
            @Valid @RequestBody JobApprovalDto.RejectJobRequest request,
            Authentication authentication) {
        log.info("Rejecting job with id: {}", id);

        try {
            User rejector = userService.getCurrentUser(authentication);
            JobApprovalDto job = superAdminService.rejectJob(id, rejector, request.getReason());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job rejected successfully");
            response.put("data", job);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== JOB APPLICATIONS APIs ====================

    @GetMapping("/job-applications")
    @Operation(summary = "Get All Job Applications", description = "Get all job applications with pagination")
    public ResponseEntity<Map<String, Object>> getAllJobApplications(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "Status filter") @RequestParam(required = false) String status) {

        log.info("Fetching all job applications - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SuperAdminJobApplicationDto> applications = superAdminService.getAllJobApplications(pageable, status);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Job applications retrieved successfully");
        response.put("data", applications.getContent());
        response.put("currentPage", applications.getNumber());
        response.put("totalItems", applications.getTotalElements());
        response.put("totalPages", applications.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/job-applications/{id}")
    @Operation(summary = "Get Job Application Details", description = "Get detailed information of a specific job application")
    public ResponseEntity<Map<String, Object>> getJobApplicationDetails(@PathVariable Long id) {
        log.info("Fetching job application details for id: {}", id);

        try {
            SuperAdminJobApplicationDto application = superAdminService.getJobApplicationById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job application details retrieved successfully");
            response.put("data", application);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== AUDITION APPLICATIONS APIs ====================

    @GetMapping("/audition-applications")
    @Operation(summary = "Get All Audition Applications", description = "Get all casting call/audition applications")
    public ResponseEntity<Map<String, Object>> getAllAuditionApplications(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "Status filter") @RequestParam(required = false) String status) {

        log.info("Fetching all audition applications - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SuperAdminJobApplicationDto> applications = superAdminService.getAllAuditionApplications(pageable, status);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Audition applications retrieved successfully");
        response.put("data", applications.getContent());
        response.put("currentPage", applications.getNumber());
        response.put("totalItems", applications.getTotalElements());
        response.put("totalPages", applications.getTotalPages());

        return ResponseEntity.ok(response);
    }

    // ==================== INTERVIEWS APIs ====================

    @GetMapping("/interviews")
    @Operation(summary = "Get All Interviews", description = "Get all scheduled interviews")
    public ResponseEntity<Map<String, Object>> getAllInterviews(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "interviewScheduledAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "ASC") String sortDir) {

        log.info("Fetching all interviews - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SuperAdminInterviewDto> interviews = superAdminService.getAllInterviews(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Interviews retrieved successfully");
        response.put("data", interviews.getContent());
        response.put("currentPage", interviews.getNumber());
        response.put("totalItems", interviews.getTotalElements());
        response.put("totalPages", interviews.getTotalPages());

        return ResponseEntity.ok(response);
    }

    // ==================== ARTIST PORTFOLIO APIs ====================

    @GetMapping("/artists/{id}/portfolio")
    @Operation(summary = "Get Artist Portfolio", description = "Get complete portfolio of a specific artist")
    public ResponseEntity<Map<String, Object>> getArtistPortfolio(@PathVariable Long id) {
        log.info("Fetching artist portfolio for id: {}", id);

        try {
            SuperAdminArtistPortfolioDto portfolio = superAdminService.getArtistPortfolio(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Artist portfolio retrieved successfully");
            response.put("data", portfolio);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== REPORT CONTENT APIs ====================

    @GetMapping("/report-content")
    @Operation(summary = "Get All Reports", description = "Get all content reports with pagination")
    public ResponseEntity<Map<String, Object>> getAllReportContent(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "Status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Priority filter") @RequestParam(required = false) String priority) {

        log.info("Fetching all reports - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SuperAdminReportContentDto> reports = superAdminService.getAllReports(pageable, status, priority);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Reports retrieved successfully");
        response.put("data", reports.getContent());
        response.put("currentPage", reports.getNumber());
        response.put("totalItems", reports.getTotalElements());
        response.put("totalPages", reports.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/report-content/{id}")
    @Operation(summary = "Get Report Details", description = "Get detailed information of a specific report")
    public ResponseEntity<Map<String, Object>> getReportDetails(@PathVariable Long id) {
        log.info("Fetching report details for id: {}", id);

        try {
            SuperAdminReportContentDto report = superAdminService.getReportById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Report details retrieved successfully");
            response.put("data", report);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/report-content/{id}/review")
    @Operation(summary = "Review Report", description = "Review and take action on a report")
    public ResponseEntity<Map<String, Object>> reviewReport(
            @PathVariable Long id,
            @Valid @RequestBody SuperAdminReportContentDto.ReviewReportRequest request,
            Authentication authentication) {
        log.info("Reviewing report with id: {}", id);

        try {
            User reviewer = userService.getCurrentUser(authentication);
            SuperAdminReportContentDto report = superAdminService.reviewReport(id, request, reviewer);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Report reviewed successfully");
            response.put("data", report);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== CATEGORIES APIs ====================

    @GetMapping("/categories")
    @Operation(summary = "Get All Categories", description = "Get all artist type categories")
    public ResponseEntity<Map<String, Object>> getAllCategories(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "sortOrder") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "ASC") String sortDir) {

        log.info("Fetching all categories - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SuperAdminCategoryDto> categories = superAdminService.getAllCategories(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Categories retrieved successfully");
        response.put("data", categories.getContent());
        response.put("currentPage", categories.getNumber());
        response.put("totalItems", categories.getTotalElements());
        response.put("totalPages", categories.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories/{id}")
    @Operation(summary = "Get Category Details", description = "Get detailed information of a specific category")
    public ResponseEntity<Map<String, Object>> getCategoryDetails(@PathVariable Long id) {
        log.info("Fetching category details for id: {}", id);

        try {
            SuperAdminCategoryDto category = superAdminService.getCategoryById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category details retrieved successfully");
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/categories")
    @Operation(summary = "Create Category", description = "Create a new artist type category")
    public ResponseEntity<Map<String, Object>> createCategory(
            @Valid @RequestBody SuperAdminCategoryDto.CreateCategoryRequest request) {
        log.info("Creating new category: {}", request.getName());

        try {
            SuperAdminCategoryDto category = superAdminService.createCategory(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category created successfully");
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update Category", description = "Update an existing category")
    public ResponseEntity<Map<String, Object>> updateCategory(
            @PathVariable Long id,
            @RequestBody SuperAdminCategoryDto.UpdateCategoryRequest request) {
        log.info("Updating category with id: {}", id);

        try {
            SuperAdminCategoryDto category = superAdminService.updateCategory(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category updated successfully");
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete Category", description = "Delete a category")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable Long id) {
        log.info("Deleting category with id: {}", id);

        try {
            superAdminService.deleteCategory(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category deleted successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== SKILLS APIs ====================

    @GetMapping("/skills")
    @Operation(summary = "Get All Skills", description = "Get all skills used by artists and jobs")
    public ResponseEntity<Map<String, Object>> getAllSkills() {
        log.info("Fetching all skills");

        List<SuperAdminSkillDto> skills = superAdminService.getAllSkills();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Skills retrieved successfully");
        response.put("data", skills);
        response.put("totalItems", skills.size());

        return ResponseEntity.ok(response);
    }
}
