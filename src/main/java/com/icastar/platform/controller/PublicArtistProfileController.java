package com.icastar.platform.controller;

import com.icastar.platform.dto.ArtistProfileFieldDto;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.ArtistProfileArtistType;
import com.icastar.platform.entity.User;
import com.icastar.platform.repository.ArtistProfileArtistTypeRepository;
import com.icastar.platform.service.ArtistService;
import com.icastar.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/artists")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public", description = "Public API endpoints - No authentication required")
public class PublicArtistProfileController {

    private final ArtistService artistService;
    private final UserService userService;
    private final ArtistProfileArtistTypeRepository artistProfileArtistTypeRepository;

    /**
     * Get public artist profile by user ID
     * GET /api/public/artists/{userId}/profile
     * No authentication required
     */
    @Operation(summary = "Get public artist profile", description = "Get artist profile by user ID - No authentication required")
    @GetMapping("/{userId}/profile")
    public ResponseEntity<Map<String, Object>> getArtistProfile(@PathVariable Long userId) {
        try {
            log.info("Fetching public artist profile for user ID: {}", userId);

            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            // Initialize lazy-loaded fields
            if (artistProfile.getArtistType() != null) {
                artistProfile.getArtistType().getName();
            }

            // Get dynamic fields
            List<ArtistProfileFieldDto> dynamicFields = artistService.getDynamicFields(artistProfile.getId());

            // Build complete profile response (same structure as /artists/profile/complete)
            Map<String, Object> profileData = new HashMap<>();

            // User info
            profileData.put("userId", user.getId());
            profileData.put("email", user.getEmail());
            profileData.put("firstName", user.getFirstName());
            profileData.put("lastName", user.getLastName());
            profileData.put("mobile", user.getMobile());
            profileData.put("isOnboardingComplete", user.getIsOnboardingComplete());

            // Artist profile info
            profileData.put("artistProfileId", artistProfile.getId());
            profileData.put("stageName", artistProfile.getStageName());
            profileData.put("bio", artistProfile.getBio());
            profileData.put("dateOfBirth", artistProfile.getDateOfBirth());
            profileData.put("gender", artistProfile.getGender());
            profileData.put("location", artistProfile.getLocation());
            profileData.put("maritalStatus", artistProfile.getMaritalStatus());
            profileData.put("languagesSpoken", artistProfile.getLanguagesSpoken());
            profileData.put("comfortableAreas", artistProfile.getComfortableAreas());
            profileData.put("projectsWorked", artistProfile.getProjectsWorked());
            profileData.put("skills", artistProfile.getSkills());
            profileData.put("experienceYears", artistProfile.getExperienceYears());
            profileData.put("weight", artistProfile.getWeight());
            profileData.put("height", artistProfile.getHeight());
            profileData.put("hairColor", artistProfile.getHairColor());
            profileData.put("hairLength", artistProfile.getHairLength());
            profileData.put("hasTattoo", artistProfile.getHasTattoo());
            profileData.put("hasMole", artistProfile.getHasMole());
            profileData.put("shoeSize", artistProfile.getShoeSize());
            profileData.put("eyeColor", artistProfile.getEyeColor());
            profileData.put("complexion", artistProfile.getComplexion());
            profileData.put("hasPassport", artistProfile.getHasPassport());
            profileData.put("travelCities", artistProfile.getTravelCities());
            profileData.put("hourlyRate", artistProfile.getHourlyRate());
            profileData.put("photoUrl", artistProfile.getPhotoUrl());
            profileData.put("videoUrl", artistProfile.getVideoUrl());
            profileData.put("portfolioUrls", artistProfile.getPortfolioUrls());
            profileData.put("danceShowreelUrl", artistProfile.getDanceShowreelUrl());
            profileData.put("profileUrl", artistProfile.getProfileUrl());
            profileData.put("coverPhotoUrl", artistProfile.getCoverPhotoUrl());
            profileData.put("isVerified", artistProfile.getIsVerifiedBadge());
            profileData.put("isProfileComplete", artistProfile.getIsProfileComplete());
            profileData.put("totalApplications", artistProfile.getTotalApplications());
            profileData.put("successfulHires", artistProfile.getSuccessfulHires());

            // Primary artist type (backward compatibility)
            if (artistProfile.getArtistType() != null) {
                Map<String, Object> artistTypeData = new HashMap<>();
                artistTypeData.put("id", artistProfile.getArtistType().getId());
                artistTypeData.put("name", artistProfile.getArtistType().getName());
                artistTypeData.put("displayName", artistProfile.getArtistType().getDisplayName());
                artistTypeData.put("description", artistProfile.getArtistType().getDescription());
                profileData.put("artistType", artistTypeData);
            }

            // All artist types (new field for multiple professions)
            List<ArtistProfileArtistType> allArtistTypes = artistProfileArtistTypeRepository
                    .findByArtistProfileIdOrderBySortOrder(artistProfile.getId());

            if (allArtistTypes != null && !allArtistTypes.isEmpty()) {
                List<Map<String, Object>> artistTypesList = new ArrayList<>();
                for (ArtistProfileArtistType apat : allArtistTypes) {
                    Map<String, Object> typeData = new HashMap<>();
                    typeData.put("id", apat.getArtistType().getId());
                    typeData.put("name", apat.getArtistType().getName());
                    typeData.put("displayName", apat.getArtistType().getDisplayName());
                    artistTypesList.add(typeData);
                }
                profileData.put("artistTypes", artistTypesList);
            } else if (artistProfile.getArtistType() != null) {
                // Fallback: if no entries in join table, use the primary artist type
                List<Map<String, Object>> artistTypesList = new ArrayList<>();
                Map<String, Object> typeData = new HashMap<>();
                typeData.put("id", artistProfile.getArtistType().getId());
                typeData.put("name", artistProfile.getArtistType().getName());
                typeData.put("displayName", artistProfile.getArtistType().getDisplayName());
                artistTypesList.add(typeData);
                profileData.put("artistTypes", artistTypesList);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", profileData);
            response.put("dynamicFields", dynamicFields);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error getting public artist profile for user ID {}: {}", userId, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error getting public artist profile for user ID {}: {}", userId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(500).body(response);
        }
    }
}
