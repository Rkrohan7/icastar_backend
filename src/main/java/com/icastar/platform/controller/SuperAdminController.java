package com.icastar.platform.controller;

import com.icastar.platform.dto.superadmin.*;
import com.icastar.platform.entity.Job;
import com.icastar.platform.service.SuperAdminService;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/super-admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Super Admin Dashboard, Reports & Management APIs")
@PreAuthorize("hasRole('ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

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
}
