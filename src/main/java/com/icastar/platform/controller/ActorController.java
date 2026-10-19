package com.icastar.platform.controller;

import com.icastar.platform.dto.artist.CreateArtistProfileDto;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.User;
import com.icastar.platform.service.ArtistService;
import com.icastar.platform.service.ArtistProfileService;
import com.icastar.platform.service.UserService;
import com.icastar.platform.service.ActorService;
import com.icastar.platform.service.DocumentService;
import com.icastar.platform.repository.ArtistProfileRepository;
import com.icastar.platform.repository.ArtistTypeFieldRepository;
import com.icastar.platform.dto.ArtistProfileFieldDto;
import com.icastar.platform.entity.ArtistTypeField;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/actors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Actors", description = "Actor-specific profile management and operations")
public class ActorController {

    private final ActorService actorService;
    private final ArtistService artistService;
    private final ArtistProfileService artistProfileService;
    private final UserService userService;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistTypeFieldRepository artistTypeFieldRepository;
    private final ObjectMapper objectMapper;
    private final DocumentService documentService;

    @Operation(
            summary = "Actor Signup",
            description = "Register a new actor with all required actor-specific fields including profile pictures, comfortable areas, acting videos, and documents.",
            operationId = "actorSignup"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Actor registered successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = "{\"success\": true, \"message\": \"Actor registered successfully\", \"data\": {...}}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or validation error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Error Response",
                                    value = "{\"success\": false, \"message\": \"Validation failed\", \"errors\": [...]}"
                            )
                    )
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> actorSignup(
            @Parameter(description = "Actor signup details", required = true)
            @Validated({Default.class, CreateArtistProfileDto.RequiredFields.class})
            @RequestBody CreateArtistProfileDto request) {
        try {
            // For actor signup, we need to create a user first
            // This should be handled by the existing user creation logic
            // For now, we'll assume the user is already created
            User user = new User();
            user.setEmail("actor@example.com"); // This should come from request
            user.setMobile("+919876543210"); // This should come from request
            user.setPassword("password123"); // This should come from request
            user.setRole(User.UserRole.ARTIST);
            user.setStatus(User.UserStatus.ACTIVE);
            user.setIsVerified(true);
            user.setFailedLoginAttempts(0);

            // Create actor profile with all required fields
            ArtistProfile actorProfile = actorService.createActorProfile(user, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Actor registered successfully");
            response.put("data", Map.of(
                    "id", actorProfile.getId(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "email", user.getEmail(),
                    "mobile", user.getMobile(),
                    "role", user.getRole(),
                    "isVerified", user.getIsVerified()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during actor signup", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Actor registration failed");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(
            summary = "Update Actor Profile",
            description = "Update actor profile with all actor-specific fields. Requires authentication.",
            operationId = "updateActorProfile"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Actor profile updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = "{\"success\": true, \"message\": \"Actor profile updated successfully\", \"data\": {...}}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or validation error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            )
    })
    @PutMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> updateActorProfile(
            @Parameter(description = "Actor profile update details", required = true)
            @Valid @RequestBody CreateArtistProfileDto request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile existingProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Actor profile not found"));

            // Update actor profile
            ArtistProfile updatedProfile = actorService.updateActorProfile(existingProfile.getId(), request);
            artistProfileService.evictProfileCaches(user.getId(), existingProfile.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Actor profile updated successfully");
            response.put("data", updatedProfile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating actor profile", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update actor profile");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(
            summary = "Update Complete Actor Profile",
            description = "Update actor profile with all required fields including basic info, documents, physical attributes, and acting-specific fields. Accepts multipart form data with text fields and file uploads.",
            operationId = "updateCompleteActorProfile"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Actor profile updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = "{\"success\": true, \"message\": \"Actor profile updated successfully\", \"data\": {...}}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or validation error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            )
    })
    @PutMapping("/profile/complete")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> updateCompleteActorProfile(
            // Text fields
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "stageName", required = false) String stageName,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam(value = "dateOfBirth", required = false) String dateOfBirth,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "maritalStatus", required = false) String maritalStatus,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "languagesSpoken", required = false) String languagesSpoken,
            @RequestParam(value = "experienceYears", required = false) Integer experienceYears,
            @RequestParam(value = "hourlyRate", required = false) Double hourlyRate,
            @RequestParam(value = "weight", required = false) Double weight,
            @RequestParam(value = "height", required = false) Double height,
            @RequestParam(value = "hairColor", required = false) String hairColor,
            @RequestParam(value = "hairLength", required = false) String hairLength,
            @RequestParam(value = "hasTattoo", required = false) Boolean hasTattoo,
            @RequestParam(value = "hasMole", required = false) Boolean hasMole,
            @RequestParam(value = "shoeSize", required = false) String shoeSize,
            @RequestParam(value = "comfortableAreas", required = false) String comfortableAreas,
            @RequestParam(value = "travelCities", required = false) String travelCities,
            @RequestParam(value = "projectsWorked", required = false) String projectsWorked,

            // File uploads
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "profilePictures", required = false) List<MultipartFile> profilePictures,
            @RequestParam(value = "profilePictureTypes", required = false) List<String> profilePictureTypes,
            @RequestParam(value = "passport", required = false) MultipartFile passport,
            @RequestParam(value = "aadhar", required = false) MultipartFile aadhar,
            @RequestParam(value = "pan", required = false) MultipartFile pan,
            @RequestParam(value = "idSizePic", required = false) MultipartFile idSizePic,
            @RequestParam(value = "actingVideos", required = false) List<MultipartFile> actingVideos) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile existingProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Actor profile not found"));

            // Update user fields
            User profileUser = existingProfile.getUser();
            if (firstName != null) {
                profileUser.setFirstName(firstName);
            }
            if (lastName != null) {
                profileUser.setLastName(lastName);
            }
            userService.save(profileUser);
            if (stageName != null) {
                existingProfile.setStageName(stageName);
            }
            if (bio != null) {
                existingProfile.setBio(bio);
            }
            if (dateOfBirth != null) {
                existingProfile.setDateOfBirth(java.time.LocalDate.parse(dateOfBirth));
            }
            if (gender != null) {
                existingProfile.setGender(ArtistProfile.Gender.valueOf(gender));
            }
            if (location != null) {
                existingProfile.setLocation(location);
            }
            if (experienceYears != null) {
                existingProfile.setExperienceYears(experienceYears);
            }
            if (hourlyRate != null) {
                existingProfile.setHourlyRate(hourlyRate);
            }

            // Handle file uploads using Document service
            Map<String, Object> uploadResults = new HashMap<>();

            // Upload profile image
            if (profileImage != null && !profileImage.isEmpty()) {
                com.icastar.platform.entity.Document profileImageDoc = 
                    documentService.uploadDocument(user, profileImage, com.icastar.platform.entity.Document.DocumentType.PROFILE_ID);
                uploadResults.put("profileImage", profileImageDoc);
            }

            // Upload profile pictures (front, left, right)
            if (profilePictures != null && !profilePictures.isEmpty()) {
                List<com.icastar.platform.entity.Document.DocumentType> types = new ArrayList<>();
                if (profilePictureTypes != null) {
                    for (String typeStr : profilePictureTypes) {
                        try {
                            switch (typeStr.toUpperCase()) {
                                case "FRONT_PROFILE":
                                    types.add(com.icastar.platform.entity.Document.DocumentType.PROFILE_FRONT);
                                    break;
                                case "LEFT_PROFILE":
                                    types.add(com.icastar.platform.entity.Document.DocumentType.PROFILE_LEFT);
                                    break;
                                case "RIGHT_PROFILE":
                                    types.add(com.icastar.platform.entity.Document.DocumentType.PROFILE_RIGHT);
                                    break;
                                default:
                                    log.warn("Invalid profile picture type: {}", typeStr);
                            }
                        } catch (Exception e) {
                            log.warn("Error parsing profile picture type: {}", typeStr, e);
                        }
                    }
                }
                
                List<com.icastar.platform.entity.Document> profilePicturesDocs = 
                    documentService.uploadProfilePictures(user, profilePictures, types);
                uploadResults.put("profilePictures", profilePicturesDocs);
            }

            // Upload documents
            if (passport != null && !passport.isEmpty()) {
                com.icastar.platform.entity.Document passportDoc = 
                    documentService.uploadDocument(user, passport, com.icastar.platform.entity.Document.DocumentType.PASSPORT);
                uploadResults.put("passport", passportDoc);
            }

            if (aadhar != null && !aadhar.isEmpty()) {
                com.icastar.platform.entity.Document aadharDoc = 
                    documentService.uploadDocument(user, aadhar, com.icastar.platform.entity.Document.DocumentType.AADHAR);
                uploadResults.put("aadhar", aadharDoc);
            }

            if (pan != null && !pan.isEmpty()) {
                com.icastar.platform.entity.Document panDoc = 
                    documentService.uploadDocument(user, pan, com.icastar.platform.entity.Document.DocumentType.PAN);
                uploadResults.put("pan", panDoc);
            }

            if (idSizePic != null && !idSizePic.isEmpty()) {
                com.icastar.platform.entity.Document idSizePicDoc = 
                    documentService.uploadDocument(user, idSizePic, com.icastar.platform.entity.Document.DocumentType.ID_SIZE_PIC);
                uploadResults.put("idSizePic", idSizePicDoc);
            }

            // Upload acting videos
            if (actingVideos != null && !actingVideos.isEmpty()) {
                List<com.icastar.platform.entity.Document> actingVideoDocs = 
                    documentService.uploadMultipleDocuments(user, actingVideos, com.icastar.platform.entity.Document.DocumentType.ACTING_VIDEO);
                uploadResults.put("actingVideos", actingVideoDocs);
            }

            // Save basic profile
            ArtistProfile savedProfile = artistProfileRepository.save(existingProfile);

            // Update dynamic fields (actor-specific text fields)
            List<ArtistProfileFieldDto> dynamicFields = createActorDynamicFieldsFromParams(
                    weight, height, hairColor, hairLength, hasTattoo, hasMole, shoeSize,
                    comfortableAreas, travelCities, languagesSpoken, projectsWorked
            );
            artistService.saveDynamicFields(savedProfile.getId(), dynamicFields);

            // Get updated dynamic fields
            List<ArtistProfileFieldDto> updatedDynamicFields = artistService.getDynamicFields(savedProfile.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Complete actor profile updated successfully");
            response.put("data", Map.of(
                    "profile", savedProfile,
                    "dynamicFields", updatedDynamicFields,
                    "uploadedFiles", uploadResults
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating complete actor profile", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update complete actor profile");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(
            summary = "Get Actor Profile",
            description = "Get current actor's profile with all actor-specific fields. Requires authentication.",
            operationId = "getActorProfile"
    )
    @GetMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> getActorProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile actorProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Actor profile not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", actorProfile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting actor profile", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get actor profile");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(
            summary = "Get Actor Profile by ID",
            description = "Get actor profile by ID for public viewing.",
            operationId = "getActorProfileById"
    )
    @GetMapping("/profile/{id}")
    public ResponseEntity<Map<String, Object>> getActorProfileById(@PathVariable Long id) {
        try {
            ArtistProfile actorProfile = artistService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Actor profile not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", actorProfile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting actor profile by ID", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get actor profile");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(
            summary = "Upload Actor Files",
            description = "Upload files for actor profile including profile images, documents, and acting videos.",
            operationId = "uploadActorFiles"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Files uploaded successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = "{\"success\": true, \"message\": \"Files uploaded successfully\", \"data\": {...}}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid file or upload error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            )
    })
    @PostMapping("/profile/upload")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> uploadActorFiles(
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "profilePictures", required = false) List<MultipartFile> profilePictures,
            @RequestParam(value = "profilePictureTypes", required = false) List<String> profilePictureTypes,
            @RequestParam(value = "passport", required = false) MultipartFile passport,
            @RequestParam(value = "aadhar", required = false) MultipartFile aadhar,
            @RequestParam(value = "pan", required = false) MultipartFile pan,
            @RequestParam(value = "idSizePic", required = false) MultipartFile idSizePic,
            @RequestParam(value = "actingVideos", required = false) List<MultipartFile> actingVideos) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile existingProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Actor profile not found"));

            Map<String, Object> uploadResults = new HashMap<>();

            // Upload profile image
            if (profileImage != null && !profileImage.isEmpty()) {
                com.icastar.platform.entity.Document profileImageDoc = 
                    documentService.uploadDocument(user, profileImage, com.icastar.platform.entity.Document.DocumentType.PROFILE_ID);
                uploadResults.put("profileImage", profileImageDoc);
            }

            // Upload profile pictures (front, left, right)
            if (profilePictures != null && !profilePictures.isEmpty()) {
                List<com.icastar.platform.entity.Document.DocumentType> types = new ArrayList<>();
                if (profilePictureTypes != null) {
                    for (String typeStr : profilePictureTypes) {
                        try {
                            switch (typeStr.toUpperCase()) {
                                case "FRONT_PROFILE":
                                    types.add(com.icastar.platform.entity.Document.DocumentType.PROFILE_FRONT);
                                    break;
                                case "LEFT_PROFILE":
                                    types.add(com.icastar.platform.entity.Document.DocumentType.PROFILE_LEFT);
                                    break;
                                case "RIGHT_PROFILE":
                                    types.add(com.icastar.platform.entity.Document.DocumentType.PROFILE_RIGHT);
                                    break;
                                default:
                                    log.warn("Invalid profile picture type: {}", typeStr);
                            }
                        } catch (Exception e) {
                            log.warn("Error parsing profile picture type: {}", typeStr, e);
                        }
                    }
                }
                
                List<com.icastar.platform.entity.Document> profilePicturesDocs = 
                    documentService.uploadProfilePictures(user, profilePictures, types);
                uploadResults.put("profilePictures", profilePicturesDocs);
            }

            // Upload documents
            if (passport != null && !passport.isEmpty()) {
                com.icastar.platform.entity.Document passportDoc = 
                    documentService.uploadDocument(user, passport, com.icastar.platform.entity.Document.DocumentType.PASSPORT);
                uploadResults.put("passport", passportDoc);
            }

            if (aadhar != null && !aadhar.isEmpty()) {
                com.icastar.platform.entity.Document aadharDoc = 
                    documentService.uploadDocument(user, aadhar, com.icastar.platform.entity.Document.DocumentType.AADHAR);
                uploadResults.put("aadhar", aadharDoc);
            }

            if (pan != null && !pan.isEmpty()) {
                com.icastar.platform.entity.Document panDoc = 
                    documentService.uploadDocument(user, pan, com.icastar.platform.entity.Document.DocumentType.PAN);
                uploadResults.put("pan", panDoc);
            }

            if (idSizePic != null && !idSizePic.isEmpty()) {
                com.icastar.platform.entity.Document idSizePicDoc = 
                    documentService.uploadDocument(user, idSizePic, com.icastar.platform.entity.Document.DocumentType.ID_SIZE_PIC);
                uploadResults.put("idSizePic", idSizePicDoc);
            }

            // Upload acting videos
            if (actingVideos != null && !actingVideos.isEmpty()) {
                List<com.icastar.platform.entity.Document> actingVideoDocs = 
                    documentService.uploadMultipleDocuments(user, actingVideos, com.icastar.platform.entity.Document.DocumentType.ACTING_VIDEO);
                uploadResults.put("actingVideos", actingVideoDocs);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Files uploaded successfully");
            response.put("data", uploadResults);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading actor files", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to upload files");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Create dynamic fields for actor profile from the request
     */
    private List<ArtistProfileFieldDto> createActorDynamicFields(CreateArtistProfileDto request) {
        List<ArtistProfileFieldDto> dynamicFields = new ArrayList<>();

        try {
            // Get actor type fields
            List<ArtistTypeField> actorFields = artistTypeFieldRepository.findActiveFieldsByArtistTypeName("ACTOR");

            // Map request data to dynamic fields
            for (ArtistTypeField field : actorFields) {
                ArtistProfileFieldDto fieldDto = new ArtistProfileFieldDto();
                fieldDto.setArtistTypeFieldId(field.getId());
                fieldDto.setFieldName(field.getFieldName());
                fieldDto.setDisplayName(field.getDisplayName());

                // Map values based on field name
                String fieldValue = getFieldValue(request, field.getFieldName());
                if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                    fieldDto.setFieldValue(fieldValue);
                    dynamicFields.add(fieldDto);
                }
            }

        } catch (Exception e) {
            log.error("Error creating dynamic fields", e);
        }

        return dynamicFields;
    }

    /**
     * Create dynamic fields from multipart parameters
     */
    private List<ArtistProfileFieldDto> createActorDynamicFieldsFromParams(
            Double weight, Double height, String hairColor, String hairLength,
            Boolean hasTattoo, Boolean hasMole, String shoeSize,
            String comfortableAreas, String travelCities, String languagesSpoken, String projectsWorked) {

        List<ArtistProfileFieldDto> dynamicFields = new ArrayList<>();

        try {
            // Get actor type fields
            List<ArtistTypeField> actorFields = artistTypeFieldRepository.findActiveFieldsByArtistTypeName("ACTOR");

            // Map parameter data to dynamic fields
            for (ArtistTypeField field : actorFields) {
                ArtistProfileFieldDto fieldDto = new ArtistProfileFieldDto();
                fieldDto.setArtistTypeFieldId(field.getId());
                fieldDto.setFieldName(field.getFieldName());
                fieldDto.setDisplayName(field.getDisplayName());

                // Map values based on field name
                String fieldValue = getFieldValueFromParams(field.getFieldName(), weight, height, hairColor,
                        hairLength, hasTattoo, hasMole, shoeSize, comfortableAreas, travelCities, languagesSpoken, projectsWorked);

                if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                    fieldDto.setFieldValue(fieldValue);
                    dynamicFields.add(fieldDto);
                }
            }

        } catch (Exception e) {
            log.error("Error creating dynamic fields from parameters", e);
        }

        return dynamicFields;
    }

    /**
     * Get field value from parameters based on field name
     */
    private String getFieldValueFromParams(String fieldName, Double weight, Double height, String hairColor,
                                           String hairLength, Boolean hasTattoo, Boolean hasMole, String shoeSize,
                                           String comfortableAreas, String travelCities, String languagesSpoken, String projectsWorked) {
        try {
            switch (fieldName) {
                case "height":
                    return height != null ? height.toString() : null;
                case "weight":
                    return weight != null ? weight.toString() : null;
                case "hair_color":
                    return hairColor;
                case "hair_length":
                    return hairLength;
                case "has_tattoo":
                    return hasTattoo != null ? hasTattoo.toString() : null;
                case "has_mole":
                    return hasMole != null ? hasMole.toString() : null;
                case "shoe_size":
                    return shoeSize;
                case "comfortable_areas":
                    return comfortableAreas;
                case "travel_cities":
                    return travelCities;
                case "languages_spoken":
                    return languagesSpoken;
                case "projects_worked":
                    return projectsWorked;
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("Error converting field value for field: " + fieldName, e);
            return null;
        }
    }

    /**
     * Get field value from request based on field name
     */
    private String getFieldValue(CreateArtistProfileDto request, String fieldName) {
        try {
            switch (fieldName) {
                case "height":
                    return request.getHeight() != null ? request.getHeight().toString() : null;
                case "weight":
                    return request.getWeight() != null ? request.getWeight().toString() : null;
                case "hair_color":
                    return request.getHairColor();
                case "hair_length":
                    return request.getHairLength();
                case "has_tattoo":
                    return request.getHasTattoo() != null ? request.getHasTattoo().toString() : null;
                case "has_mole":
                    return request.getHasMole() != null ? request.getHasMole().toString() : null;
                case "shoe_size":
                    return request.getShoeSize();
                case "comfortable_areas":
                    return request.getComfortableAreas() != null ?
                            objectMapper.writeValueAsString(request.getComfortableAreas()) : null;
                case "travel_cities":
                    return request.getTravelCities() != null ?
                            objectMapper.writeValueAsString(request.getTravelCities()) : null;
                case "languages_spoken":
                    return request.getLanguagesSpoken() != null ?
                            objectMapper.writeValueAsString(request.getLanguagesSpoken()) : null;
                case "years_experience":
                    return request.getExperienceYears() != null ?
                            request.getExperienceYears().toString() : null;
                case "projects_worked":
                    return request.getProjectsWorked() != null ?
                            objectMapper.writeValueAsString(request.getProjectsWorked()) : null;
                // Document-related fields (profile pictures, acting videos, passport, aadhar, pan, id_size_pic) 
                // are now handled by Document entity
                default:
                    return null;
            }
        } catch (JsonProcessingException e) {
            log.error("Error converting field value to JSON for field: " + fieldName, e);
            return null;
        }
    }
}