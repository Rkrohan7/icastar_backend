package com.icastar.platform.service;

import com.icastar.platform.dto.admin.AccountManagementResponseDto;
import com.icastar.platform.dto.admin.AccountStatusChangeDto;
import com.icastar.platform.entity.*;
import com.icastar.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAccountService {
    
    private final UserRepository userRepository;
    private final AccountManagementLogRepository accountManagementLogRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final AccountStatusHistoryRepository accountStatusHistoryRepository;
    
    /**
     * Check if user has account management permissions
     */
    private void checkAccountManagementPermission(User admin, AdminPermission.PermissionLevel requiredLevel) {
        if (admin.getRole() == User.UserRole.ADMIN && admin.getAdminRole() == User.AdminRole.SUPER_ADMIN) {
            return;
        }
        boolean permitted = adminPermissionRepository
                .findByUserIdAndPermissionTypeAndIsActiveTrue(
                        admin.getId(), AdminPermission.PermissionType.ACCOUNT_MANAGEMENT)
                .stream()
                .filter(permission -> permission.getExpiresAt() == null
                        || permission.getExpiresAt().isAfter(LocalDateTime.now()))
                .anyMatch(permission -> hasPermissionLevel(permission.getPermissionLevel(), requiredLevel));
        if (!permitted) {
            throw new RuntimeException("Insufficient permissions for account management");
        }
    }

    private boolean hasPermissionLevel(
            AdminPermission.PermissionLevel actual, AdminPermission.PermissionLevel required) {
        return switch (actual) {
            case READ -> required == AdminPermission.PermissionLevel.READ;
            case WRITE -> required == AdminPermission.PermissionLevel.READ
                    || required == AdminPermission.PermissionLevel.WRITE;
            case ADMIN, SUPER_ADMIN -> true;
        };
    }
    
    /**
     * Get all users with filtering and pagination
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(String status, String role, String search, Pageable pageable, User admin) {
        checkAccountManagementPermission(admin, AdminPermission.PermissionLevel.READ);
        
        // This would need to be implemented in UserRepository with custom query
        // For now, returning all users - implement filtering in repository
        return userRepository.findAll(pageable);
    }
    
    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public AccountManagementResponseDto getUserById(Long userId, User admin) {
        checkAccountManagementPermission(admin, AdminPermission.PermissionLevel.READ);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return AccountManagementResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .currentStatus(user.getAccountStatus().name())
                .lastActivity(user.getLastActivity())
                .deactivatedAt(user.getDeactivatedAt())
                .reactivatedAt(user.getReactivatedAt())
                .isActive(user.getAccountStatus() == User.AccountStatus.ACTIVE)
                .role(user.getRole().name())
                .build();
    }
    
    /**
     * Deactivate user account
     */
    @Transactional
    public AccountManagementResponseDto deactivateAccount(AccountStatusChangeDto request, User admin) {
        checkAccountManagementPermission(admin, AdminPermission.PermissionLevel.WRITE);
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        User.AccountStatus previousStatus = user.getAccountStatus();
        user.setAccountStatus(User.AccountStatus.INACTIVE);
        user.setDeactivatedAt(LocalDateTime.now());
        user.setDeactivatedBy(admin);
        user.setDeactivationReason(request.getReason());
        user.setLastActivity(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        // Log the action
        logAccountAction(user, admin, AccountManagementLog.AccountAction.DEACTIVATE, 
                        previousStatus, User.AccountStatus.INACTIVE, request);
        
        // Add to status history
        addStatusHistory(user, User.AccountStatus.INACTIVE, admin, request.getReason(), request.getAdminNotes());
        
        return buildResponseDto(savedUser);
    }
    
    /**
     * Reactivate user account
     */
    @Transactional
    public AccountManagementResponseDto reactivateAccount(AccountStatusChangeDto request, User admin) {
        checkAccountManagementPermission(admin, AdminPermission.PermissionLevel.WRITE);
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        User.AccountStatus previousStatus = user.getAccountStatus();
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        user.setReactivatedAt(LocalDateTime.now());
        user.setReactivatedBy(admin);
        user.setReactivationReason(request.getReason());
        user.setLastActivity(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        // Log the action
        logAccountAction(user, admin, AccountManagementLog.AccountAction.ACTIVATE, 
                        previousStatus, User.AccountStatus.ACTIVE, request);
        
        // Add to status history
        addStatusHistory(user, User.AccountStatus.ACTIVE, admin, request.getReason(), request.getAdminNotes());
        
        return buildResponseDto(savedUser);
    }
    
    /**
     * Suspend user account
     */
    @Transactional
    public AccountManagementResponseDto suspendAccount(AccountStatusChangeDto request, User admin) {
        checkAccountManagementPermission(admin, AdminPermission.PermissionLevel.WRITE);
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        User.AccountStatus previousStatus = user.getAccountStatus();
        user.setAccountStatus(User.AccountStatus.SUSPENDED);
        user.setDeactivatedAt(LocalDateTime.now());
        user.setDeactivatedBy(admin);
        user.setDeactivationReason(request.getReason());
        user.setLastActivity(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        // Log the action
        logAccountAction(user, admin, AccountManagementLog.AccountAction.SUSPEND, 
                        previousStatus, User.AccountStatus.SUSPENDED, request);
        
        // Add to status history
        addStatusHistory(user, User.AccountStatus.SUSPENDED, admin, request.getReason(), request.getAdminNotes());
        
        return buildResponseDto(savedUser);
    }
    
    /**
     * Ban user account
     */
    @Transactional
    public AccountManagementResponseDto banAccount(AccountStatusChangeDto request, User admin) {
        checkAccountManagementPermission(admin, AdminPermission.PermissionLevel.WRITE);
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        User.AccountStatus previousStatus = user.getAccountStatus();
        user.setAccountStatus(User.AccountStatus.BANNED);
        user.setDeactivatedAt(LocalDateTime.now());
        user.setDeactivatedBy(admin);
        user.setDeactivationReason(request.getReason());
        user.setLastActivity(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        // Log the action
        logAccountAction(user, admin, AccountManagementLog.AccountAction.BAN, 
                        previousStatus, User.AccountStatus.BANNED, request);
        
        // Add to status history
        addStatusHistory(user, User.AccountStatus.BANNED, admin, request.getReason(), request.getAdminNotes());
        
        return buildResponseDto(savedUser);
    }
    
    /**
     * Get account management logs
     */
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getAccountLogs(Long userId, Pageable pageable, User admin) {
        checkAccountManagementPermission(admin, AdminPermission.PermissionLevel.READ);
        
        Page<AccountManagementLog> logs = accountManagementLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return logs.map(log -> {
            Map<String, Object> logMap = new HashMap<>();
            logMap.put("id", log.getId());
            logMap.put("action", log.getAction().name());
            logMap.put("previousStatus", log.getPreviousStatus().name());
            logMap.put("newStatus", log.getNewStatus().name());
            logMap.put("reason", log.getReason());
            logMap.put("adminNotes", log.getAdminNotes());
            logMap.put("adminName", log.getAdmin().getFirstName() + " " + log.getAdmin().getLastName());
            logMap.put("createdAt", log.getCreatedAt());
            logMap.put("ipAddress", log.getIpAddress());
            return logMap;
        });
    }
    
    /**
     * Get account statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAccountStatistics(User admin) {
        checkAccountManagementPermission(admin, AdminPermission.PermissionLevel.READ);
        
        Map<String, Object> statistics = new HashMap<>();
        
        // Count users by status
        long activeUsers = userRepository.countByAccountStatus(User.AccountStatus.ACTIVE);
        long inactiveUsers = userRepository.countByAccountStatus(User.AccountStatus.INACTIVE);
        long suspendedUsers = userRepository.countByAccountStatus(User.AccountStatus.SUSPENDED);
        long bannedUsers = userRepository.countByAccountStatus(User.AccountStatus.BANNED);
        long pendingUsers = userRepository.countByAccountStatus(User.AccountStatus.PENDING_VERIFICATION);
        
        statistics.put("activeUsers", activeUsers);
        statistics.put("inactiveUsers", inactiveUsers);
        statistics.put("suspendedUsers", suspendedUsers);
        statistics.put("bannedUsers", bannedUsers);
        statistics.put("pendingUsers", pendingUsers);
        statistics.put("totalUsers", activeUsers + inactiveUsers + suspendedUsers + bannedUsers + pendingUsers);
        
        // Recent account changes
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        long recentChanges = accountManagementLogRepository.countByCreatedAtAfter(last24Hours);
        statistics.put("recentChanges", recentChanges);
        
        return statistics;
    }
    
    /**
     * Log account action
     */
    private void logAccountAction(User user, User admin, AccountManagementLog.AccountAction action, 
                                 User.AccountStatus previousStatus, User.AccountStatus newStatus, 
                                 AccountStatusChangeDto request) {
        
        AccountManagementLog log = AccountManagementLog.builder()
                .user(user)
                .admin(admin)
                .action(action)
                .previousStatus(AccountManagementLog.AccountStatus.valueOf(previousStatus.name()))
                .newStatus(AccountManagementLog.AccountStatus.valueOf(newStatus.name()))
                .reason(request.getReason())
                .adminNotes(request.getAdminNotes())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .build();
        
        accountManagementLogRepository.save(log);
        log.info("Account action logged: {} by admin {} for user {}", action, admin.getId(), user.getId());
    }
    
    /**
     * Add status history
     */
    private void addStatusHistory(User user, User.AccountStatus status, User admin, String reason, String notes) {
        AccountStatusHistory history = AccountStatusHistory.builder()
                .user(user)
                .status(AccountStatusHistory.AccountStatus.valueOf(status.name()))
                .changedBy(admin)
                .reason(reason)
                .notes(notes)
                .effectiveDate(LocalDateTime.now())
                .build();
        
        accountStatusHistoryRepository.save(history);
    }
    
    /**
     * Build response DTO
     */
    private AccountManagementResponseDto buildResponseDto(User user) {
        return AccountManagementResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .currentStatus(user.getAccountStatus().name())
                .lastActivity(user.getLastActivity())
                .deactivatedAt(user.getDeactivatedAt())
                .reactivatedAt(user.getReactivatedAt())
                .isActive(user.getAccountStatus() == User.AccountStatus.ACTIVE)
                .role(user.getRole().name())
                .build();
    }
}
