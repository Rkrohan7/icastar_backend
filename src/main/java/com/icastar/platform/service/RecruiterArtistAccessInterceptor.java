package com.icastar.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icastar.platform.entity.RecruiterProfile;
import com.icastar.platform.entity.User;
import com.icastar.platform.repository.RecruiterProfileRepository;
import com.icastar.platform.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.http.server.PathContainer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecruiterArtistAccessInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final ObjectMapper objectMapper;
    private final PathPatternParser pathPatternParser = new PathPatternParser();

    private final List<PathPattern> protectedPaths = List.of(
            pattern("/recruiter/dashboard"),
            pattern("/recruiter/dashboard/jobs/*/applicants"),
            pattern("/recruiter/dashboard/jobs/*/applications"),
            pattern("/recruiter/dashboard/artists"),
            pattern("/recruiter/dashboard/suggestions"),
            pattern("/recruiter/dashboard/hires"),
            pattern("/recruiter/dashboard/view-applicants/*"),
            pattern("/recruiter/dashboard/latest-applicants"),
            pattern("/recruiter/dashboard/applications-trend"),
            pattern("/recruiter/dashboard/application-status"),
            pattern("/recruiter/dashboard/interview-outcomes"),
            pattern("/recruiter/dashboard/interview-result"),
            pattern("/recruiter/dashboard/schedule-interview"),
            pattern("/recruiter/dashboard/project-report"),
            pattern("/recruiter/applications/**"),
            pattern("/recruiter/hire-requests/**"),
            pattern("/recruiter/auditions/*/applications/**"),
            pattern("/artist-profiles/**"),
            pattern("/artists/profile/*"),
            pattern("/artists/search"),
            pattern("/artists/type/*"),
            pattern("/artists/verified")
    );

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String servletPath = request.getServletPath();
        if (!requiresRecruiterVerification(servletPath)) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null || user.getRole() != User.UserRole.RECRUITER) {
            return true;
        }

        RecruiterProfile profile = recruiterProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile != null && profile.hasApprovedFaceAndIdProof()) {
            return true;
        }

        List<String> missingRequirements = new ArrayList<>();
        if (profile == null
                || profile.getFaceVerificationStatus() != RecruiterProfile.VerificationStatus.APPROVED
                || !Boolean.TRUE.equals(profile.getIsFaceVerified())) {
            missingRequirements.add("FACE_VERIFICATION_REQUIRED");
        }
        if (profile == null
                || profile.getIdProofVerificationStatus() != RecruiterProfile.VerificationStatus.APPROVED
                || !Boolean.TRUE.equals(profile.getIdProofVerified())) {
            missingRequirements.add("ID_PROOF_REQUIRED");
        }

        log.warn("Blocked recruiter {} from artist data at {}: {}", user.getId(), servletPath, missingRequirements);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("errorCode", "RECRUITER_VERIFICATION_REQUIRED");
        body.put("message", "Face and ID proof verification must both be approved to access artist data.");
        body.put("missingRequirements", missingRequirements);
        objectMapper.writeValue(response.getWriter(), body);
        return false;
    }

    private boolean requiresRecruiterVerification(String servletPath) {
        return protectedPaths.stream().anyMatch(pattern -> pattern.matches(
                PathContainer.parsePath(servletPath)));
    }

    private PathPattern pattern(String path) {
        return pathPatternParser.parse(path);
    }
}
