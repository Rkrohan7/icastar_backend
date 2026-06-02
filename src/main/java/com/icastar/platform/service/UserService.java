package com.icastar.platform.service;

import com.icastar.platform.config.CacheNames;
import com.icastar.platform.dto.user.UpdateUserStatusDto;
import com.icastar.platform.entity.User;
import com.icastar.platform.exception.BusinessException;
import com.icastar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.USER_BY_ID, key = "#id", unless = "#result == null || !#result.isPresent()")
    public Optional<User> findById(Long id) {
        log.debug("Cache MISS: Loading user by id: {}", id);
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.USER_BY_EMAIL, key = "#email", unless = "#result == null || !#result.isPresent()")
    public Optional<User> findByEmail(String email) {
        log.debug("Cache MISS: Loading user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByMobile(String mobile) {
        return userRepository.findByMobile(mobile);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmailOrMobile(String email, String mobile) {
        return userRepository.findByEmailOrMobile(email, mobile);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.USERS_BY_ROLE, key = "#role.name()")
    public List<User> findByRole(User.UserRole role) {
        log.debug("Cache MISS: Loading users by role: {}", role);
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public List<User> findByStatus(User.UserStatus status) {
        return userRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Page<User> findByRoleAndStatus(User.UserRole role, User.UserStatus status, Pageable pageable) {
        return userRepository.findByRoleAndStatus(role, status, pageable);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_BY_ID, key = "#user.id", condition = "#user.id != null"),
        @CacheEvict(value = CacheNames.USER_BY_EMAIL, key = "#user.email", condition = "#user.email != null"),
        @CacheEvict(value = CacheNames.USERS_BY_ROLE, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    })
    public User save(User user) {
        log.debug("Saving user - Cache will be evicted");
        return userRepository.save(user);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_BY_ID, key = "#userId"),
        @CacheEvict(value = CacheNames.USER_BY_EMAIL, allEntries = true),
        @CacheEvict(value = CacheNames.USERS_BY_ROLE, allEntries = true)
    })
    public User updateUserStatus(Long userId, User.UserStatus status) {
        log.info("Updating user {} status to {} - Cache will be evicted", userId, status);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setStatus(status);
        return userRepository.save(user);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_BY_ID, key = "#userId"),
        @CacheEvict(value = CacheNames.USER_BY_EMAIL, allEntries = true)
    })
    public User updateUserVerification(Long userId, Boolean isVerified) {
        log.info("Updating user {} verification to {} - Cache will be evicted", userId, isVerified);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setIsVerified(isVerified);
        return userRepository.save(user);
    }

    public User updateLastLogin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        user.setLastLogin(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        return userRepository.save(user);
    }

    public User incrementFailedLoginAttempts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        
        // Lock account after 5 failed attempts for 30 minutes
        if (user.getFailedLoginAttempts() >= 5) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
        }
        
        return userRepository.save(user);
    }

    public User unlockAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        return userRepository.save(user);
    }


    @Transactional(readOnly = true)
    public List<User> findLockedAccounts() {
        return userRepository.findLockedAccounts(5, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Long countByRoleAndCreatedAtAfter(User.UserRole role, LocalDateTime startDate) {
        return userRepository.countByRoleAndCreatedAtAfter(role, startDate);
    }

    @Transactional(readOnly = true)
    public List<User> findByEmailContainingOrMobileContaining(String email, String mobile) {
        return userRepository.findByEmailContainingOrMobileContaining(email, mobile);
    }

    @Transactional(readOnly = true)
    public List<User> findActiveUsersByRoles(List<User.UserRole> roles) {
        return userRepository.findActiveUsersByRoles(roles);
    }

    @Transactional(readOnly = true)
    public List<User> findUsersByLastLoginBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return userRepository.findUsersByLastLoginBetween(startDate, endDate);
    }

    // Enhanced User Management Methods

    @Transactional(readOnly = true)
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> findUsersByRole(User.UserRole role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> findUsersByStatus(User.UserStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> findUsersByRoleAndStatus(User.UserRole role, User.UserStatus status, Pageable pageable) {
        return userRepository.findByRoleAndStatus(role, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.findByEmailContainingIgnoreCaseOrMobileContaining(searchTerm, searchTerm, pageable);
    }

    public User updateUserStatus(Long userId, UpdateUserStatusDto updateDto) {
        User user = findById(userId)
                .orElseThrow(() -> new BusinessException("User not found with id: " + userId));

        log.info("Updating user status for user {} from {} to {}", 
                userId, user.getStatus(), updateDto.getStatus());

        user.setStatus(updateDto.getStatus());
        
        if (updateDto.getIsVerified() != null) {
            user.setIsVerified(updateDto.getIsVerified());
        }
        
        if (updateDto.getAccountLockedUntil() != null) {
            user.setAccountLockedUntil(updateDto.getAccountLockedUntil());
        }

        User updatedUser = userRepository.save(user);
        log.info("User status updated successfully for user {}", userId);
        
        return updatedUser;
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_BY_ID, key = "#userId"),
        @CacheEvict(value = CacheNames.USER_BY_EMAIL, allEntries = true),
        @CacheEvict(value = CacheNames.USERS_BY_ROLE, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    })
    public User banUser(Long userId, String reason) {
        User user = findById(userId)
                .orElseThrow(() -> new BusinessException("User not found with id: " + userId));

        log.info("Banning user {} with reason: {} - Cache will be evicted", userId, reason);

        user.setStatus(User.UserStatus.BANNED);
        user.setAccountLockedUntil(LocalDateTime.now().plusYears(1)); // Ban for 1 year

        User bannedUser = userRepository.save(user);
        log.info("User {} banned successfully", userId);

        return bannedUser;
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_BY_ID, key = "#userId"),
        @CacheEvict(value = CacheNames.USER_BY_EMAIL, allEntries = true),
        @CacheEvict(value = CacheNames.USERS_BY_ROLE, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    })
    public User unbanUser(Long userId) {
        User user = findById(userId)
                .orElseThrow(() -> new BusinessException("User not found with id: " + userId));

        log.info("Unbanning user {} - Cache will be evicted", userId);

        user.setStatus(User.UserStatus.ACTIVE);
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);

        User unbannedUser = userRepository.save(user);
        log.info("User {} unbanned successfully", userId);

        return unbannedUser;
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_BY_ID, key = "#userId"),
        @CacheEvict(value = CacheNames.USER_BY_EMAIL, allEntries = true)
    })
    public User verifyUser(Long userId) {
        User user = findById(userId)
                .orElseThrow(() -> new BusinessException("User not found with id: " + userId));

        log.info("Verifying user {} - Cache will be evicted", userId);

        user.setIsVerified(true);

        User verifiedUser = userRepository.save(user);
        log.info("User {} verified successfully", userId);

        return verifiedUser;
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_BY_ID, key = "#userId"),
        @CacheEvict(value = CacheNames.USER_BY_EMAIL, allEntries = true)
    })
    public User unverifyUser(Long userId) {
        User user = findById(userId)
                .orElseThrow(() -> new BusinessException("User not found with id: " + userId));

        log.info("Unverifying user {} - Cache will be evicted", userId);

        user.setIsVerified(false);

        User unverifiedUser = userRepository.save(user);
        log.info("User {} unverified successfully", userId);

        return unverifiedUser;
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_BY_ID, key = "#userId"),
        @CacheEvict(value = CacheNames.USER_BY_EMAIL, allEntries = true),
        @CacheEvict(value = CacheNames.USERS_BY_ROLE, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    })
    public void deleteUser(Long userId) {
        User user = findById(userId)
                .orElseThrow(() -> new BusinessException("User not found with id: " + userId));

        log.info("Deleting user {} - Cache will be evicted", userId);

        // Soft delete - set as inactive
        user.setIsActive(false);
        user.setStatus(User.UserStatus.INACTIVE);

        userRepository.save(user);
        log.info("User {} deleted successfully", userId);
    }

    @Transactional(readOnly = true)
    public Long getTotalUsersCount() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public Long getUsersCountByRole(User.UserRole role) {
        return userRepository.countByRole(role);
    }

    @Transactional(readOnly = true)
    public Long getUsersCountByStatus(User.UserStatus status) {
        return userRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public Long getVerifiedUsersCount() {
        return userRepository.countByIsVerified(true);
    }

    @Transactional(readOnly = true)
    public List<User> getRecentlyRegisteredUsers(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return userRepository.findByCreatedAtAfter(cutoffDate);
    }

    @Transactional(readOnly = true)
    public List<User> getActiveUsersByLastLogin(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return userRepository.findByLastLoginAfterAndStatus(cutoffDate, User.UserStatus.ACTIVE);
    }

    /**
     * Change user password after verifying current password
     * @param userId User ID
     * @param currentPassword Current password
     * @param newPassword New password
     * @param confirmPassword Confirmation of new password
     * @throws BusinessException if validation fails
     */
    public void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        User user = findById(userId)
                .orElseThrow(() -> new BusinessException("User not found with id: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("Failed password change attempt for user {}: incorrect current password", userId);
            throw new BusinessException("Current password is incorrect");
        }

        // Check that new password and confirm password match
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("New password and confirm password do not match");
        }

        // Check that new password is different from current password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("New password must be different from current password");
        }

        // Encode and save new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for user {}", userId);
    }
}
