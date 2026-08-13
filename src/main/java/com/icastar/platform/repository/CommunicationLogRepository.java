package com.icastar.platform.repository;

import com.icastar.platform.entity.CommunicationLog;
import com.icastar.platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, Long> {

    // Find logs by user
    List<CommunicationLog> findByUserOrderByCreatedAtDesc(User user);
    
    Page<CommunicationLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Find logs by communication type
    List<CommunicationLog> findByCommunicationTypeOrderByCreatedAtDesc(CommunicationLog.CommunicationType type);
    
    Page<CommunicationLog> findByCommunicationTypeOrderByCreatedAtDesc(
        CommunicationLog.CommunicationType type, Pageable pageable);

    // Find logs by status
    List<CommunicationLog> findByStatusOrderByCreatedAtDesc(CommunicationLog.CommunicationStatus status);
    
    Page<CommunicationLog> findByStatusOrderByCreatedAtDesc(
        CommunicationLog.CommunicationStatus status, Pageable pageable);

    // Find logs by recipient email
    List<CommunicationLog> findByRecipientEmailOrderByCreatedAtDesc(String email);
    
    Page<CommunicationLog> findByRecipientEmailOrderByCreatedAtDesc(String email, Pageable pageable);

    // Find logs by recipient mobile
    List<CommunicationLog> findByRecipientMobileOrderByCreatedAtDesc(String mobile);
    
    Page<CommunicationLog> findByRecipientMobileOrderByCreatedAtDesc(String mobile, Pageable pageable);

    // Find failed communications that can be retried
    @Query("SELECT cl FROM CommunicationLog cl WHERE cl.status = 'FAILED' AND cl.retryCount < cl.maxRetries AND (cl.nextRetryAt IS NULL OR cl.nextRetryAt <= :now)")
    List<CommunicationLog> findRetryableCommunications(@Param("now") LocalDateTime now);

    // Find logs by date range
    @Query("SELECT cl FROM CommunicationLog cl WHERE cl.createdAt BETWEEN :startDate AND :endDate ORDER BY cl.createdAt DESC")
    List<CommunicationLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT cl FROM CommunicationLog cl WHERE cl.createdAt BETWEEN :startDate AND :endDate ORDER BY cl.createdAt DESC")
    Page<CommunicationLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate, 
                                         Pageable pageable);

    // Find logs by user and communication type
    List<CommunicationLog> findByUserAndCommunicationTypeOrderByCreatedAtDesc(
        User user, CommunicationLog.CommunicationType type);
    
    Page<CommunicationLog> findByUserAndCommunicationTypeOrderByCreatedAtDesc(
        User user, CommunicationLog.CommunicationType type, Pageable pageable);

    // Find logs by template name
    List<CommunicationLog> findByTemplateNameOrderByCreatedAtDesc(String templateName);
    
    Page<CommunicationLog> findByTemplateNameOrderByCreatedAtDesc(String templateName, Pageable pageable);

    // Count communications by status
    @Query("SELECT cl.status, COUNT(cl) FROM CommunicationLog cl GROUP BY cl.status")
    List<Object[]> countByStatus();

    // Count communications by type
    @Query("SELECT cl.communicationType, COUNT(cl) FROM CommunicationLog cl GROUP BY cl.communicationType")
    List<Object[]> countByType();

    // Find recent communications for a user
    @Query("SELECT cl FROM CommunicationLog cl WHERE cl.user = :user AND cl.createdAt >= :since ORDER BY cl.createdAt DESC")
    List<CommunicationLog> findRecentByUser(@Param("user") User user, @Param("since") LocalDateTime since);

    // Find communications by external ID (for tracking with providers)
    Optional<CommunicationLog> findByExternalId(String externalId);

    // Find failed communications for a specific user
    List<CommunicationLog> findByUserAndStatusOrderByCreatedAtDesc(User user, CommunicationLog.CommunicationStatus status);

    // Get communication statistics
    @Query("SELECT " +
           "COUNT(CASE WHEN cl.status = 'DELIVERED' THEN 1 END) as delivered, " +
           "COUNT(CASE WHEN cl.status = 'FAILED' THEN 1 END) as failed, " +
           "COUNT(CASE WHEN cl.status = 'SENT' THEN 1 END) as sent, " +
           "COUNT(*) as total " +
           "FROM CommunicationLog cl WHERE cl.createdAt >= :since")
    Object[] getCommunicationStats(@Param("since") LocalDateTime since);

    // Check if a reminder was sent to this email within the time period
    @Query("SELECT COUNT(cl) > 0 FROM CommunicationLog cl WHERE cl.recipientEmail = :email AND cl.templateName = :templateName AND cl.createdAt >= :since AND (cl.status = 'SENT' OR cl.status = 'DELIVERED')")
    boolean existsByRecipientEmailAndTemplateNameAndCreatedAtAfter(@Param("email") String email, @Param("templateName") String templateName, @Param("since") LocalDateTime since);
}
