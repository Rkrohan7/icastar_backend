package com.icastar.platform.controller;

import com.icastar.platform.dto.upload.PresignedUrlRequestDto;
import com.icastar.platform.dto.upload.PresignedUrlResponseDto;
import com.icastar.platform.entity.User;
import com.icastar.platform.service.ArtistProfileService;
import com.icastar.platform.service.RecruiterDashboardService;
import com.icastar.platform.service.S3Service;
import com.icastar.platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final S3Service s3Service;
    private final UserService userService;
    private final ArtistProfileService artistProfileService;
    private final RecruiterDashboardService recruiterDashboardService;

    /**
     * Generate presigned URL for S3 upload
     * POST /api/upload/presigned-url
     *
     * @param request   PresignedUrlRequestDto containing fileName, fileType, uploadType
     * @param authentication Authentication object
     * @return Presigned URL and file details
     */
    @PostMapping("/presigned-url")
    public ResponseEntity<Map<String, Object>> getPresignedUrl(
            @Valid @RequestBody PresignedUrlRequestDto request,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Generating presigned URL for user: {}, uploadType: {}", email, request.getUploadType());

            // BLOG_IMAGE validation: admin only, image types only
            if ("BLOG_IMAGE".equalsIgnoreCase(request.getUploadType())) {
                // Check if user is admin
                if (!User.UserRole.ADMIN.equals(user.getRole())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Only admins can upload blog images");
                    return ResponseEntity.status(403).body(response);
                }

                // Validate content type (only image/jpeg, image/png, image/webp allowed)
                String contentType = request.getFileType();
                if (contentType == null ||
                        (!contentType.equals("image/jpeg") &&
                         !contentType.equals("image/png") &&
                         !contentType.equals("image/webp"))) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Only JPEG, PNG, and WebP images are allowed for blog images");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            PresignedUrlResponseDto presignedUrlResponse = s3Service.generatePresignedUrl(request, user);

            // Auto-save fileUrl to database based on uploadType
            String uploadType = request.getUploadType();
            if (uploadType != null) {
                String fileUrl = presignedUrlResponse.getFileUrl();
                switch (uploadType.toUpperCase()) {
                    case "FACE_VERIFICATION":
                        artistProfileService.updateFaceVerificationUrl(user.getId(), fileUrl);
                        log.info("Saved face verification URL for user: {}", email);
                        break;
                    case "PROFILE_PHOTO":
                    case "ARTIST_PROFILE":
                        // Check user role and update appropriate profile
                        if (User.UserRole.RECRUITER.equals(user.getRole())) {
                            recruiterDashboardService.updateProfilePhotoUrl(user.getId(), fileUrl);
                            log.info("Saved profile photo URL for recruiter: {}", email);
                        } else {
                            artistProfileService.updateProfilePhotoUrl(user.getId(), fileUrl);
                            log.info("Saved profile photo URL for artist: {}", email);
                        }
                        break;
                    case "COVER_PHOTO":
                        artistProfileService.updateCoverPhotoUrl(user.getId(), fileUrl);
                        log.info("Saved cover photo URL for user: {}", email);
                        break;
                    case "ID_PROOF":
                        artistProfileService.updateIdProofUrl(user.getId(), fileUrl);
                        log.info("Saved ID proof URL for user: {}", email);
                        break;
                    case "DANCE_SHOWREEL":
                        artistProfileService.updateDanceShowreelUrl(user.getId(), fileUrl);
                        log.info("Saved dance showreel URL for user: {}", email);
                        break;
                    default:
                        log.info("No auto-save for uploadType: {}", uploadType);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", presignedUrlResponse);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating presigned URL: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to generate presigned URL: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Upload artist profile photo
     * POST /api/upload/artist-profile-photo
     *
     * @param file           The image file to upload
     * @param authentication Authentication object
     * @return The uploaded file URL
     */
    @PostMapping(value = "/artist-profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadArtistProfilePhoto(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            // Validate file
            if (file.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "File is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file type (only images allowed)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Only image files are allowed");
                return ResponseEntity.badRequest().body(response);
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Uploading artist profile photo for user: {}", email);

            // Upload to S3
            String fileUrl = s3Service.uploadFile(file, "ARTIST_PROFILE_PHOTO", user.getId());

            // Update profile_url in artist_profiles table
            artistProfileService.updateProfilePhotoUrl(user.getId(), fileUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile photo uploaded successfully");
            response.put("data", Map.of(
                    "profileUrl", fileUrl
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading artist profile photo: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to upload profile photo: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Upload artist cover photo
     * POST /api/upload/cover-photo
     */
    @PostMapping(value = "/cover-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadCoverPhoto(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authentication required"));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File is required"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Only image files are allowed"));
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Uploading cover photo for user: {}", email);

            String fileUrl = s3Service.uploadFile(file, "COVER_PHOTO", user.getId());
            artistProfileService.updateCoverPhotoUrl(user.getId(), fileUrl);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cover photo uploaded successfully",
                    "data", Map.of("coverPhotoUrl", fileUrl)
            ));

        } catch (Exception e) {
            log.error("Error uploading cover photo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to upload cover photo: " + e.getMessage()));
        }
    }

    /**
     * Upload artist ID proof
     * POST /api/upload/id-proof
     */
    @PostMapping(value = "/id-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadIdProof(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authentication required"));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File is required"));
            }

            // Allow images and PDFs for ID proof
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Only image or PDF files are allowed"));
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Uploading ID proof for user: {}", email);

            String fileUrl = s3Service.uploadFile(file, "ID_PROOF", user.getId());
            artistProfileService.updateIdProofUrl(user.getId(), fileUrl);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ID proof uploaded successfully",
                    "data", Map.of("idProofUrl", fileUrl)
            ));

        } catch (Exception e) {
            log.error("Error uploading ID proof: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to upload ID proof: " + e.getMessage()));
        }
    }

    /**
     * Upload artist dance showreel
     * POST /api/upload/dance-showreel
     */
    @PostMapping(value = "/dance-showreel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDanceShowreel(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authentication required"));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File is required"));
            }

            // Allow video files for dance showreel
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Only video files are allowed"));
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Uploading dance showreel for user: {}", email);

            String fileUrl = s3Service.uploadFile(file, "DANCE_SHOWREEL", user.getId());
            artistProfileService.updateDanceShowreelUrl(user.getId(), fileUrl);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Dance showreel uploaded successfully",
                    "data", Map.of("danceShowreelUrl", fileUrl)
            ));

        } catch (Exception e) {
            log.error("Error uploading dance showreel: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to upload dance showreel: " + e.getMessage()));
        }
    }

    /**
     * Confirm presigned URL upload and save URL to database
     * POST /api/upload/confirm
     *
     * Call this endpoint after successfully uploading a file to S3 using presigned URL
     * to save the file URL in the database.
     */
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirmUpload(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authentication required"));
            }

            String fileUrl = request.get("fileUrl");
            String uploadType = request.get("uploadType");

            if (fileUrl == null || fileUrl.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "fileUrl is required"));
            }
            if (uploadType == null || uploadType.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "uploadType is required"));
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Confirming upload for user: {}, uploadType: {}", email, uploadType);

            String savedUrl;
            switch (uploadType.toUpperCase()) {
                case "FACE_VERIFICATION":
                    savedUrl = artistProfileService.updateFaceVerificationUrl(user.getId(), fileUrl);
                    break;
                case "PROFILE_PHOTO":
                case "ARTIST_PROFILE":
                    // Check user role and update appropriate profile
                    if (User.UserRole.RECRUITER.equals(user.getRole())) {
                        savedUrl = recruiterDashboardService.updateProfilePhotoUrl(user.getId(), fileUrl);
                    } else {
                        savedUrl = artistProfileService.updateProfilePhotoUrl(user.getId(), fileUrl);
                    }
                    break;
                case "COVER_PHOTO":
                    savedUrl = artistProfileService.updateCoverPhotoUrl(user.getId(), fileUrl);
                    break;
                case "ID_PROOF":
                    savedUrl = artistProfileService.updateIdProofUrl(user.getId(), fileUrl);
                    break;
                case "DANCE_SHOWREEL":
                    savedUrl = artistProfileService.updateDanceShowreelUrl(user.getId(), fileUrl);
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Unknown uploadType: " + uploadType));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Upload confirmed and URL saved successfully",
                    "data", Map.of("savedUrl", savedUrl)
            ));

        } catch (Exception e) {
            log.error("Error confirming upload: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to confirm upload: " + e.getMessage()));
        }
    }
}
