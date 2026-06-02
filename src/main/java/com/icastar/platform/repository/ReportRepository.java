package com.icastar.platform.repository;

import com.icastar.platform.entity.Report;
import com.icastar.platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // Find by status
    List<Report> findByStatus(Report.ReportStatus status);
    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);

    // Find by report type
    List<Report> findByReportType(Report.ReportType reportType);
    Page<Report> findByReportType(Report.ReportType reportType, Pageable pageable);

    // Find by reporter
    List<Report> findByReporter(User reporter);
    Page<Report> findByReporter(User reporter, Pageable pageable);

    // Find by reported user
    List<Report> findByReportedUser(User reportedUser);
    Page<Report> findByReportedUser(User reportedUser, Pageable pageable);

    // Find by priority
    List<Report> findByPriority(Report.ReportPriority priority);
    Page<Report> findByPriority(Report.ReportPriority priority, Pageable pageable);

    // Find by status and priority
    Page<Report> findByStatusAndPriority(Report.ReportStatus status, Report.ReportPriority priority, Pageable pageable);

    // Find pending reports
    @Query("SELECT r FROM Report r WHERE r.status = 'PENDING' ORDER BY r.priority DESC, r.createdAt ASC")
    List<Report> findPendingReports();

    @Query("SELECT r FROM Report r WHERE r.status = 'PENDING' ORDER BY r.priority DESC, r.createdAt ASC")
    Page<Report> findPendingReports(Pageable pageable);

    // Find reports by entity
    @Query("SELECT r FROM Report r WHERE r.entityType = :entityType AND r.entityId = :entityId")
    List<Report> findByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    // Count by status
    Long countByStatus(Report.ReportStatus status);

    // Count by priority
    Long countByPriority(Report.ReportPriority priority);

    // Count by report type
    Long countByReportType(Report.ReportType reportType);

    // Find reports created after date
    @Query("SELECT r FROM Report r WHERE r.createdAt >= :date ORDER BY r.createdAt DESC")
    List<Report> findRecentReports(@Param("date") LocalDateTime date);

    // Count reports by status and date range
    @Query("SELECT COUNT(r) FROM Report r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    Long countByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find unresolved reports by reported user
    @Query("SELECT r FROM Report r WHERE r.reportedUser.id = :userId AND r.status IN ('PENDING', 'UNDER_REVIEW')")
    List<Report> findUnresolvedByReportedUserId(@Param("userId") Long userId);
}
