package com.icastar.platform.config;

import com.icastar.platform.security.JwtAuthenticationEntryPoint;
import com.icastar.platform.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS Configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                
                // Stateless API
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(rc -> rc.requestCache(new NullRequestCache())) // stop "Saved request … to session" logs

                // 401 for unauthenticated access
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // Public vs protected
                .authorizeHttpRequests(auth -> auth
                        // ==> These servlet paths are public (context path `/api` is handled by Tomcat)
                        .requestMatchers(
                                "/auth/register",
                                "/auth/email/login",
                                "/auth/login",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/auth/verify-reset-token",
                                "/public/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/error",
                                "/auth/forgot-password/email",
                                "/auth/reset-password/email"
                        ).permitAll()

                        // CORS preflight is always public
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Super Admin routes require ADMIN role
                        .requestMatchers("/super-admin/**").hasRole("ADMIN")

                        // Everything else requires auth
                        .anyRequest().authenticated()
                )

                // Ensure your JWT filter actually runs
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
