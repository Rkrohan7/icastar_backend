package com.icastar.platform.dto.auth;

import com.icastar.platform.dto.superadmin.AdminUserDto;
import com.icastar.platform.entity.User;
import lombok.Data;

import java.util.List;

@Data
public class AuthResponseDto {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String mobile;
    private User.UserRole role;
    private User.AdminRole adminRole;
    private List<AdminUserDto.PermissionDto> permissions = List.of();
    private User.UserStatus status;
    private Boolean isVerified;
    private Boolean isOnboardingComplete;
    private String message;

    public AuthResponseDto(String token, User user) {
        this.token = token;
        this.id = user.getId();
        this.email = user.getEmail();
        this.mobile = user.getMobile();
        this.role = user.getRole();
        this.adminRole = user.getRole() == User.UserRole.ADMIN ? user.getAdminRole() : null;
        this.status = user.getStatus();
        this.isVerified = user.getIsVerified();
        this.isOnboardingComplete = user.getIsOnboardingComplete();
    }
    
    public AuthResponseDto(String message) {
        this.message = message;
    }
}
