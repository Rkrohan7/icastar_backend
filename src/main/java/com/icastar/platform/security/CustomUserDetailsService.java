package com.icastar.platform.security;

import com.icastar.platform.entity.User;
import com.icastar.platform.entity.AdminPermission;
import com.icastar.platform.repository.AdminPermissionRepository;
import com.icastar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminPermissionRepository adminPermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrMobile(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email or mobile: " + username));

        return createUserPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return createUserPrincipal(user);
    }

    private UserDetails createUserPrincipal(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        if (user.getRole() == User.UserRole.ADMIN) {
            User.AdminRole adminRole = user.getAdminRole() == null
                    ? User.AdminRole.ADMIN : user.getAdminRole();
            authorities.add(new SimpleGrantedAuthority("ADMIN_ROLE_" + adminRole.name()));
            if (adminRole == User.AdminRole.SUPER_ADMIN) {
                for (AdminPermission.PermissionType type : AdminPermission.PermissionType.values()) {
                    authorities.add(new SimpleGrantedAuthority("PERMISSION_" + type.name() + "_SUPER_ADMIN"));
                }
            } else {
                for (AdminPermission permission : adminPermissionRepository.findByUserIdAndIsActiveTrue(user.getId())) {
                    if (permission.getExpiresAt() == null
                            || permission.getExpiresAt().isAfter(java.time.LocalDateTime.now())) {
                        authorities.add(new SimpleGrantedAuthority(
                                "PERMISSION_" + permission.getPermissionType().name()
                                        + "_" + permission.getPermissionLevel().name()));
                    }
                }
            }
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.getStatus() == User.UserStatus.BANNED || user.getStatus() == User.UserStatus.SUSPENDED)
                .credentialsExpired(false)
                .disabled(user.getStatus() != User.UserStatus.ACTIVE)
                .build();
    }
}
