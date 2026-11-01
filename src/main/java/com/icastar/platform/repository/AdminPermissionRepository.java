package com.icastar.platform.repository;

import com.icastar.platform.entity.AdminPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminPermissionRepository extends JpaRepository<AdminPermission, Long> {
    
    List<AdminPermission> findByUserIdAndIsActiveTrue(Long userId);

    List<AdminPermission> findByUserIdInAndIsActiveTrue(List<Long> userIds);
    
    List<AdminPermission> findByUserIdAndPermissionTypeAndIsActiveTrue(Long userId, AdminPermission.PermissionType permissionType);
    
    @Query("SELECT ap FROM AdminPermission ap WHERE ap.user.id = :userId AND ap.permissionType = :permissionType AND ap.permissionLevel IN ('ADMIN', 'SUPER_ADMIN') AND ap.isActive = true")
    Optional<AdminPermission> findAdminPermission(@Param("userId") Long userId, @Param("permissionType") AdminPermission.PermissionType permissionType);
    
    @Query("SELECT ap FROM AdminPermission ap WHERE ap.user.id = :userId AND ap.permissionLevel = 'SUPER_ADMIN' AND ap.isActive = true")
    List<AdminPermission> findSuperAdminPermissions(@Param("userId") Long userId);
    
    @Query("SELECT ap FROM AdminPermission ap WHERE ap.user.id = :userId AND ap.permissionType = 'ACCOUNT_MANAGEMENT' AND ap.permissionLevel IN ('ADMIN', 'SUPER_ADMIN') AND ap.isActive = true")
    Optional<AdminPermission> findAccountManagementPermission(@Param("userId") Long userId);
    
    boolean existsByUserIdAndPermissionTypeAndPermissionLevelAndIsActiveTrue(Long userId, AdminPermission.PermissionType permissionType, AdminPermission.PermissionLevel permissionLevel);
    
    Optional<AdminPermission> findByUserIdAndPermissionTypeAndPermissionLevelAndIsActiveTrue(Long userId, AdminPermission.PermissionType permissionType, AdminPermission.PermissionLevel permissionLevel);
}
