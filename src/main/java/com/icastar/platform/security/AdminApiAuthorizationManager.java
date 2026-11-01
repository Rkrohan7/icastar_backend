package com.icastar.platform.security;

import com.icastar.platform.entity.AdminPermission;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class AdminApiAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get();
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        if (authorities.contains("ADMIN_ROLE_SUPER_ADMIN")) {
            return new AuthorizationDecision(true);
        }
        if (!authorities.contains("ROLE_ADMIN")) {
            return new AuthorizationDecision(false);
        }

        AdminPermission.PermissionType permissionType = permissionTypeFor(context.getRequest().getServletPath());
        if (permissionType == null) {
            return new AuthorizationDecision(false);
        }
        boolean readOnly = "GET".equalsIgnoreCase(context.getRequest().getMethod())
                || "HEAD".equalsIgnoreCase(context.getRequest().getMethod());
        Set<AdminPermission.PermissionLevel> allowedLevels = readOnly
                ? Set.of(AdminPermission.PermissionLevel.READ, AdminPermission.PermissionLevel.WRITE,
                        AdminPermission.PermissionLevel.ADMIN, AdminPermission.PermissionLevel.SUPER_ADMIN)
                : Set.of(AdminPermission.PermissionLevel.WRITE, AdminPermission.PermissionLevel.ADMIN,
                        AdminPermission.PermissionLevel.SUPER_ADMIN);
        boolean permitted = allowedLevels.stream().anyMatch(level ->
                authorities.contains("PERMISSION_" + permissionType.name() + "_" + level.name()));
        return new AuthorizationDecision(permitted);
    }

    private AdminPermission.PermissionType permissionTypeFor(String path) {
        if (path.startsWith("/admin/accounts")) {
            return AdminPermission.PermissionType.ACCOUNT_MANAGEMENT;
        }
        if (path.startsWith("/admin/users") || path.startsWith("/admin/applications")) {
            return AdminPermission.PermissionType.USER_MANAGEMENT;
        }
        if (path.startsWith("/admin/blogs") || path.startsWith("/admin/jobs")) {
            return AdminPermission.PermissionType.CONTENT_MODERATION;
        }
        if (path.contains("/admin/communications")) {
            return AdminPermission.PermissionType.SYSTEM_ADMIN;
        }
        return null;
    }
}
