package com.icastar.platform.repository;

import com.icastar.platform.entity.User;
import com.icastar.platform.entity.User.AdminRole;
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
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMobile(String mobile);

    Optional<User> findByEmailOrMobile(String email, String mobile);

    List<User> findByRole(User.UserRole role);

    List<User> findByStatus(User.UserStatus status);

    List<User> findByIsVerified(Boolean isVerified);

    @Query("SELECT u FROM User u WHERE u.lastLogin BETWEEN :startDate AND :endDate")
    List<User> findUsersByLastLoginBetween(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = :status")
    Page<User> findByRoleAndStatus(@Param("role") User.UserRole role, 
                                  @Param("status") User.UserStatus status, 
                                  Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :maxAttempts AND u.accountLockedUntil > :currentTime")
    List<User> findLockedAccounts(@Param("maxAttempts") Integer maxAttempts, 
                                 @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.createdAt >= :startDate")
    Long countByRoleAndCreatedAtAfter(@Param("role") User.UserRole role, 
                                     @Param("startDate") LocalDateTime startDate);

    @Query("SELECT u FROM User u WHERE u.email LIKE %:email% OR u.mobile LIKE %:mobile%")
    List<User> findByEmailContainingOrMobileContaining(@Param("email") String email, 
                                                       @Param("mobile") String mobile);

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.role IN :roles")
    List<User> findActiveUsersByRoles(@Param("roles") List<User.UserRole> roles);

    @Query("SELECT u FROM User u WHERE (u.email = :email OR u.mobile = :mobile) AND u.status = 'ACTIVE'")
    Optional<User> findByEmailOrMobileAndActive(@Param("email") String email, @Param("mobile") String mobile);

    // Enhanced User Management Repository Methods

    Page<User> findByRole(User.UserRole role, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND (:status IS NULL OR u.accountStatus = :status)
              AND (:search IS NULL OR
                   LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<User> findAdminAccounts(
            @Param("role") User.UserRole role,
            @Param("status") User.AccountStatus status,
            @Param("search") String search,
            Pageable pageable);

    long countByRoleAndAdminRole(User.UserRole role, AdminRole adminRole);

    long countByRoleAndAdminRoleAndAccountStatus(
            User.UserRole role, AdminRole adminRole, User.AccountStatus accountStatus);

    Page<User> findByStatus(User.UserStatus status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.email LIKE %:searchTerm% OR u.mobile LIKE %:searchTerm%")
    Page<User> findByEmailContainingIgnoreCaseOrMobileContaining(@Param("searchTerm") String searchTerm, 
                                                                 @Param("searchTerm") String searchTerm2, 
                                                                 Pageable pageable);

    Long countByRole(User.UserRole role);

    Long countByStatus(User.UserStatus status);

    Long countByIsVerified(Boolean isVerified);

    List<User> findByCreatedAtAfter(LocalDateTime cutoffDate);

    List<User> findByLastLoginAfterAndStatus(LocalDateTime cutoffDate, User.UserStatus status);
    
    // Account Management Methods
    Long countByAccountStatus(User.AccountStatus accountStatus);
    
    List<User> findByAccountStatus(User.AccountStatus accountStatus);
    
    Page<User> findByAccountStatus(User.AccountStatus accountStatus, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.accountStatus = :status AND u.role = :role")
    Page<User> findByAccountStatusAndRole(@Param("status") User.AccountStatus status, 
                                          @Param("role") User.UserRole role, 
                                          Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.accountStatus = :status AND (u.email LIKE %:search% OR u.mobile LIKE %:search%)")
    Page<User> findByAccountStatusAndSearch(@Param("status") User.AccountStatus status, 
                                           @Param("search") String search, 
                                           Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.accountStatus = :status AND u.role = :role AND (u.email LIKE %:search% OR u.mobile LIKE %:search%)")
    Page<User> findByAccountStatusAndRoleAndSearch(@Param("status") User.AccountStatus status, 
                                                   @Param("role") User.UserRole role, 
                                                   @Param("search") String search, 
                                                   Pageable pageable);
}
