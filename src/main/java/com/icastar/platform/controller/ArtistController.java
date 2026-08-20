package com.icastar.platform.controller;

import com.icastar.platform.dto.ArtistProfileFieldDto;
import com.icastar.platform.dto.artist.CreateArtistProfileDto;
import com.icastar.platform.dto.artist.SimpleCreateArtistProfileDto;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.ArtistProfileArtistType;
import com.icastar.platform.entity.ArtistType;
import com.icastar.platform.entity.User;

import java.util.ArrayList;
import java.util.Optional;
import com.icastar.platform.repository.ArtistProfileArtistTypeRepository;
import com.icastar.platform.service.ArtistService;
import com.icastar.platform.service.ArtistProfileService;
import com.icastar.platform.service.ArtistTypeService;
import com.icastar.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.print.Pageable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/artists")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Artists", description = "Artist profile management and operations")
@SecurityRequirement(name = "bearerAuth")
public class ArtistController {

    private final ArtistService artistService;
    private final ArtistProfileService artistProfileService;
    private final ArtistTypeService artistTypeService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final ArtistProfileArtistTypeRepository artistProfileArtistTypeRepository;

    @Value("${icastar.file.upload-dir:uploads/}")
    private String uploadDir;

    @PostMapping("/profile")
    @Operation(summary = "Create or Update Artist Profile", description = "Create a new artist profile or update existing one with simplified fields")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Artist profile created/updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<Map<String, Object>> createArtistProfile(@Valid @RequestBody SimpleCreateArtistProfileDto createDto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile;
            boolean isNewProfile = false;
            String message;

            // Check if artist profile already exists - if yes, update it
            Optional<ArtistProfile> existingProfile = artistService.findByUserId(user.getId());

            if (existingProfile.isPresent()) {
                // Update existing profile
                artistProfile = existingProfile.get();
                message = "Artist profile updated successfully";
                log.info("Updating existing artist profile for user: {}", email);
            } else {
                // Create new profile
                isNewProfile = true;
                String firstName = (user.getFirstName() != null && !user.getFirstName().trim().isEmpty())
                    ? user.getFirstName().trim() : "User";
                String lastName = (user.getLastName() != null && !user.getLastName().trim().isEmpty())
                    ? user.getLastName().trim() : "Artist";

                artistProfile = artistService.createArtistProfile(
                        user.getId(),
                        createDto.getArtistTypeId(),
                        firstName,
                        lastName
                );
                message = "Artist profile created successfully";
                log.info("Creating new artist profile for user: {}", email);
            }

            // Update artistType if provided (for both create and update)
            if (createDto.getArtistTypeId() != null) {
                ArtistType artistType = artistTypeService.findById(createDto.getArtistTypeId())
                        .orElseThrow(() -> new RuntimeException("Artist type not found with id: " + createDto.getArtistTypeId()));
                artistProfile.setArtistType(artistType);
            }

            // Set the additional fields from the simplified DTO
            if (createDto.getDateOfBirth() != null) {
                artistProfile.setDateOfBirth(createDto.getDateOfBirth());
            }
            if (createDto.getGender() != null) {
                artistProfile.setGender(createDto.getGender());
            }
            if (createDto.getLocation() != null) {
                artistProfile.setLocation(createDto.getLocation());
            }
            if (createDto.getExperienceYears() != null) {
                artistProfile.setExperienceYears(createDto.getExperienceYears());
            }

            // Handle isOnboardingComplete flag
            if (createDto.getIsOnboardingComplete() != null && createDto.getIsOnboardingComplete()) {
                user.setIsOnboardingComplete(true);
                userService.save(user);
            }

            artistService.save(artistProfile);

            // Save multiple artist types to join table
            if (createDto.getArtistTypeIds() != null && !createDto.getArtistTypeIds().isEmpty()) {
                artistProfileService.updateArtistTypes(artistProfile, createDto.getArtistTypeIds());
            } else if (createDto.getArtistTypeId() != null) {
                // Backward compatibility: save single artistTypeId to join table
                artistProfileService.updateArtistTypes(artistProfile, List.of(createDto.getArtistTypeId()));
            }

            // Get dynamic fields for the response
            List<ArtistProfileFieldDto> dynamicFields = artistService.getDynamicFields(artistProfile.getId());

            // Build response with artistType details
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("id", artistProfile.getId());
            profileData.put("firstName", artistProfile.getFirstName());
            profileData.put("lastName", artistProfile.getLastName());
            profileData.put("dateOfBirth", artistProfile.getDateOfBirth());
            profileData.put("gender", artistProfile.getGender());
            profileData.put("location", artistProfile.getLocation());
            profileData.put("experienceYears", artistProfile.getExperienceYears());

            // Include artistType (role) details for audition filtering
            if (artistProfile.getArtistType() != null) {
                Map<String, Object> artistTypeData = new HashMap<>();
                artistTypeData.put("id", artistProfile.getArtistType().getId());
                artistTypeData.put("name", artistProfile.getArtistType().getName());
                artistTypeData.put("description", artistProfile.getArtistType().getDescription());
                profileData.put("artistType", artistTypeData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("isNewProfile", isNewProfile);
            response.put("data", profileData);
            response.put("dynamicFields", dynamicFields);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating/updating artist profile", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create/update artist profile");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getCurrentArtistProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            // Initialize lazy-loaded fields to avoid serialization issues
            if (artistProfile.getArtistType() != null) {
                artistProfile.getArtistType().getName(); // Trigger lazy loading
            }

            // Get dynamic fields for the response
            List<ArtistProfileFieldDto> dynamicFields = artistService.getDynamicFields(artistProfile.getId());

            // Prepare profile data with user information
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("userId", user.getId());
            profileData.put("email", user.getEmail());
            profileData.put("firstName", user.getFirstName());
            profileData.put("lastName", user.getLastName());
            profileData.put("mobile", user.getMobile());
            profileData.put("isOnboardingComplete", user.getIsOnboardingComplete());
            profileData.put("artistProfile", artistProfile);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", profileData);
            response.put("dynamicFields", dynamicFields);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting artist profile", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get artist profile");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get complete profile for current logged-in artist
     * GET /api/artists/profile/complete
     */
    @GetMapping("/profile/complete")
    public ResponseEntity<Map<String, Object>> getCompleteProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            // Initialize lazy-loaded fields
            if (artistProfile.getArtistType() != null) {
                artistProfile.getArtistType().getName();
            }

            // Get dynamic fields
            List<ArtistProfileFieldDto> dynamicFields = artistService.getDynamicFields(artistProfile.getId());

            // Build complete profile response
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
            profileData.put("idProofUrl", artistProfile.getIdProofUrl());
            profileData.put("faceVerificationUrl", artistProfile.getFaceVerificationUrl());
            profileData.put("idProofVerified", artistProfile.getIdProofVerified());
            profileData.put("isVerified", artistProfile.getIsVerifiedBadge());
            profileData.put("isProfileComplete", artistProfile.getIsProfileComplete());

            // Artist type (role) info - primary type
            if (artistProfile.getArtistType() != null) {
                Map<String, Object> artistTypeData = new HashMap<>();
                artistTypeData.put("id", artistProfile.getArtistType().getId());
                artistTypeData.put("name", artistProfile.getArtistType().getName());
                artistTypeData.put("displayName", artistProfile.getArtistType().getDisplayName());
                artistTypeData.put("description", artistProfile.getArtistType().getDescription());
                profileData.put("artistType", artistTypeData);
            }

            // All artist types (multiple professions)
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
        } catch (Exception e) {
            log.error("Error getting complete artist profile", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get complete artist profile");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<Map<String, Object>> getArtistProfile(@PathVariable Long id) {
        try {
            ArtistProfile artistProfile = artistService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            // Initialize lazy-loaded fields to avoid serialization issues
            if (artistProfile.getArtistType() != null) {
                artistProfile.getArtistType().getName(); // Trigger lazy loading
            }

            // Get dynamic fields for the response
            List<ArtistProfileFieldDto> dynamicFields = artistService.getDynamicFields(artistProfile.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", artistProfile);
            response.put("dynamicFields", dynamicFields);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting artist profile", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get artist profile");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateArtistProfile(@Valid @RequestBody CreateArtistProfileDto updateDto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile existingProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            // Create updated profile object
            // Update user fields
            User profileUser = existingProfile.getUser();
            if (updateDto.getFirstName() != null) {
                profileUser.setFirstName(updateDto.getFirstName());
            }
            if (updateDto.getLastName() != null) {
                profileUser.setLastName(updateDto.getLastName());
            }
            userService.save(profileUser);

            ArtistProfile updatedProfile = new ArtistProfile();
            updatedProfile.setStageName(updateDto.getStageName());
            updatedProfile.setBio(updateDto.getBio());
            updatedProfile.setDateOfBirth(updateDto.getDateOfBirth());
            updatedProfile.setGender(updateDto.getGender());
            updatedProfile.setLocation(updateDto.getLocation());

            if (updateDto.getSkills() != null) {
                try {
                    updatedProfile.setSkills(objectMapper.writeValueAsString(updateDto.getSkills()));
                } catch (JsonProcessingException e) {
                    log.error("Error converting skills to JSON", e);
                    throw new RuntimeException("Error processing skills data");
                }
            }
            updatedProfile.setExperienceYears(updateDto.getExperienceYears());
            updatedProfile.setHourlyRate(updateDto.getHourlyRate());

            // Physical / personal fields
            updatedProfile.setMaritalStatus(updateDto.getMaritalStatus());
            updatedProfile.setWeight(updateDto.getWeight());
            updatedProfile.setHeight(updateDto.getHeight());
            updatedProfile.setHairColor(updateDto.getHairColor());
            updatedProfile.setHairLength(updateDto.getHairLength());
            updatedProfile.setHasTattoo(updateDto.getHasTattoo());
            updatedProfile.setHasMole(updateDto.getHasMole());
            updatedProfile.setShoeSize(updateDto.getShoeSize());
            updatedProfile.setEyeColor(updateDto.getEyeColor());
            updatedProfile.setComplexion(updateDto.getComplexion());
            updatedProfile.setHasPassport(updateDto.getHasPassport());

            // Handle portfolio URLs
            updatedProfile.setPhotoUrl(updateDto.getPhotoUrl());
            updatedProfile.setVideoUrl(updateDto.getVideoUrl());
            updatedProfile.setProfileUrl(updateDto.getProfileUrl());

            // Handle other JSON fields
            if (updateDto.getLanguagesSpoken() != null) {
                try {
                    updatedProfile.setLanguagesSpoken(objectMapper.writeValueAsString(updateDto.getLanguagesSpoken()));
                } catch (JsonProcessingException e) {
                    log.error("Error converting languagesSpoken to JSON", e);
                    throw new RuntimeException("Error processing languagesSpoken data");
                }
            }
            
            if (updateDto.getComfortableAreas() != null) {
                try {
                    updatedProfile.setComfortableAreas(objectMapper.writeValueAsString(updateDto.getComfortableAreas()));
                } catch (JsonProcessingException e) {
                    log.error("Error converting comfortableAreas to JSON", e);
                    throw new RuntimeException("Error processing comfortableAreas data");
                }
            }
            
            if (updateDto.getProjectsWorked() != null) {
                try {
                    updatedProfile.setProjectsWorked(objectMapper.writeValueAsString(updateDto.getProjectsWorked()));
                } catch (JsonProcessingException e) {
                    log.error("Error converting projectsWorked to JSON", e);
                    throw new RuntimeException("Error processing projectsWorked data");
                }
            }
            
            if (updateDto.getTravelCities() != null) {
                try {
                    updatedProfile.setTravelCities(objectMapper.writeValueAsString(updateDto.getTravelCities()));
                } catch (JsonProcessingException e) {
                    log.error("Error converting travelCities to JSON", e);
                    throw new RuntimeException("Error processing travelCities data");
                }
            }

            if (updateDto.getPortfolioUrls() != null) {
                try {
                    updatedProfile.setPortfolioUrls(objectMapper.writeValueAsString(updateDto.getPortfolioUrls()));
                } catch (JsonProcessingException e) {
                    log.error("Error converting portfolioUrls to JSON", e);
                    throw new RuntimeException("Error processing portfolioUrls data");
                }
            }

            ArtistProfile savedProfile = artistService.updateArtistProfile(existingProfile.getId(), updatedProfile);

            // Handle dynamic fields if provided
            if (updateDto.getDynamicFields() != null && !updateDto.getDynamicFields().isEmpty()) {
                artistService.saveDynamicFields(existingProfile.getId(), updateDto.getDynamicFields());
            }

            List<ArtistProfileFieldDto> dynamicFields = artistService.getDynamicFields(existingProfile.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Artist profile updated successfully");
            response.put("data", savedProfile);
            response.put("dynamicFields", dynamicFields);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating artist profile", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update artist profile");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/profile/verify")
    public ResponseEntity<Map<String, Object>> requestVerificationBadge() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            artistService.requestVerificationBadge(artistProfile.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verification request submitted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error requesting verification badge", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to request verification badge");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchArtists(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) Long artistTypeId,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) Double maxRate,
            Pageable pageable) {
        try {
            List<ArtistProfile> artists = artistService.searchArtists(
                    searchTerm, location, skills, artistTypeId, minExperience, maxRate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", artists);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching artists", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to search artists");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/type/{artistTypeId}")
    public ResponseEntity<Map<String, Object>> getArtistsByType(@PathVariable Long artistTypeId) {
        try {
            List<ArtistProfile> artists = artistService.findActiveArtistsByType(artistTypeId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", artists);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting artists by type", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get artists by type");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/verified")
    public ResponseEntity<Map<String, Object>> getVerifiedArtists() {
        try {
            List<ArtistProfile> artists = artistService.findActiveVerifiedArtists();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", artists);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting verified artists", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get verified artists");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/profile/upload-portfolio-photo")
    @Operation(summary = "Upload Portfolio Photo", description = "Upload artist's portfolio photo to S3")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Photo uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or upload failed")
    })
    public ResponseEntity<Map<String, Object>> uploadPortfolioPhoto(
            @RequestParam("file") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            // Upload photo locally
            String photoUrl = saveFileLocally(file, "artists/" + artistProfile.getId() + "/portfolio/photos");

            // Update artist profile with photo URL
            artistProfile.setPhotoUrl(photoUrl);
            artistService.save(artistProfile);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Portfolio photo uploaded successfully");
            response.put("data", Map.of(
                "photoUrl", photoUrl,
                "fileName", file.getOriginalFilename(),
                "fileSize", file.getSize()
            ));

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error uploading portfolio photo", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error uploading portfolio photo", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to upload portfolio photo");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/profile/upload-portfolio-video")
    @Operation(summary = "Upload Portfolio Video", description = "Upload artist's portfolio video to S3")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Video uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or upload failed")
    })
    public ResponseEntity<Map<String, Object>> uploadPortfolioVideo(
            @RequestParam("file") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            // Upload video locally
            String videoUrl = saveFileLocally(file, "artists/" + artistProfile.getId() + "/portfolio/videos");

            // Update artist profile with video URL
            artistProfile.setVideoUrl(videoUrl);
            artistService.save(artistProfile);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Portfolio video uploaded successfully");
            response.put("data", Map.of(
                "videoUrl", videoUrl,
                "fileName", file.getOriginalFilename(),
                "fileSize", file.getSize()
            ));

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error uploading portfolio video", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error uploading portfolio video", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to upload portfolio video");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/profile/upload-profile-image")
    @Operation(summary = "Upload Profile Image", description = "Upload artist's profile image to S3")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile image uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or upload failed")
    })
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            // Upload profile image locally
            String profileImageUrl = saveFileLocally(file, "artists/" + artistProfile.getId() + "/profile");

            // Update artist profile with profile image URL
            artistProfile.setProfileUrl(profileImageUrl);
            artistService.save(artistProfile);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile image uploaded successfully");
            response.put("data", Map.of(
                "profileUrl", profileImageUrl,
                "fileName", file.getOriginalFilename(),
                "fileSize", file.getSize()
            ));

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error uploading profile image", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error uploading profile image", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to upload profile image");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Save file to local storage
     */
    private String saveFileLocally(MultipartFile file, String folder) {
        try {
            String fileName = generateFileName(file.getOriginalFilename());
            Path uploadPath = Paths.get(uploadDir, folder);
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            String fileUrl = "/" + uploadDir + folder + "/" + fileName;
            log.info("File saved locally: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            log.error("Error saving file locally", e);
            throw new RuntimeException("Failed to save file: " + e.getMessage());
        }
    }

    /**
     * Generate unique filename
     */
    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
}
