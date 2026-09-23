package com.icastar.platform.service;

import com.icastar.platform.config.S3Config;
import com.icastar.platform.dto.upload.PresignedUrlRequestDto;
import com.icastar.platform.dto.upload.PresignedUrlResponseDto;
import com.icastar.platform.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class S3Service {

    private final S3Config s3Config;
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Autowired
    public S3Service(S3Config s3Config,
                     @Autowired(required = false) S3Presigner s3Presigner,
                     @Autowired(required = false) S3Client s3Client) {
        this.s3Config = s3Config;
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
    }

    /**
     * Check if S3 is properly configured
     */
    public boolean isConfigured() {
        return s3Config.isConfigured() && s3Presigner != null;
    }

    /**
     * Generate a presigned URL for uploading a file to S3
     *
     * @param request   The presigned URL request containing fileName, fileType, uploadType
     * @param user      The authenticated user
     * @return PresignedUrlResponseDto with presigned URL and file details
     */
    public PresignedUrlResponseDto generatePresignedUrl(PresignedUrlRequestDto request, User user) {
        try {
            // Check if S3 is configured
            if (!isConfigured()) {
                throw new RuntimeException("S3 is not configured. Please set AWS credentials in application.yml");
            }
            // Generate unique upload ID and file key
            String uploadId = UUID.randomUUID().toString();
            String fileExtension = getFileExtension(request.getFileName());
            String uniqueFileName = uploadId + fileExtension;

            // Build S3 key path based on upload type
            String s3Key = buildS3Key(request.getUploadType(), user.getId(), uniqueFileName);

            // Build the PutObjectRequest
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(s3Key)
                    .contentType(request.getFileType())
                    .build();

            // Create presigned URL request
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(s3Config.getPresignedUrlExpiration()))
                    .putObjectRequest(putObjectRequest)
                    .build();

            // Generate presigned URL
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            // Build the final file URL (after upload)
            String fileUrl = buildFileUrl(s3Key);

            log.info("Generated presigned URL for user {} - uploadType: {}, key: {}",
                    user.getId(), request.getUploadType(), s3Key);

            return PresignedUrlResponseDto.builder()
                    .presignedUrl(presignedUrl)
                    .fileUrl(fileUrl)
                    .uploadId(uploadId)
                    .expiresIn(s3Config.getPresignedUrlExpiration())
                    .s3Key(s3Key)
                    .bucketName(s3Config.getBucketName())
                    .build();

        } catch (Exception e) {
            log.error("Error generating presigned URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage());
        }
    }

    /**
     * Build S3 key path based on upload type
     */
    private String buildS3Key(String uploadType, Long userId, String fileName) {
        return switch (uploadType.toUpperCase()) {
            case "PROFILE_PHOTO", "COMPANY_LOGO" -> "recruiter/profile/" + userId + "/" + fileName;
            case "ARTIST_PROFILE" -> "artist/profile/" + userId + "/" + fileName;
            case "PORTFOLIO" -> "artist/portfolio/" + userId + "/" + fileName;
            case "RESUME" -> "documents/resume/" + userId + "/" + fileName;
            case "VIDEO", "DEMO_REEL" -> "media/videos/" + userId + "/" + fileName;
            case "JOB_ATTACHMENT" -> "jobs/attachments/" + userId + "/" + fileName;
            case "BLOG_IMAGE" -> "blogs/" + fileName;
            default -> "uploads/general/" + userId + "/" + fileName;
        };
    }

    /**
     * Build the public file URL
     */
    private String buildFileUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3Config.getBucketName(),
                s3Config.getRegion(),
                s3Key);
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * Upload file directly to S3 and return the file URL
     *
     * @param file       The multipart file to upload
     * @param uploadType The type of upload (e.g., ARTIST_PROFILE_PHOTO)
     * @param userId     The user ID
     * @return The S3 file URL
     */
    public String uploadFile(MultipartFile file, String uploadType, Long userId) {
        try {
            if (!isConfigured()) {
                throw new RuntimeException("S3 is not configured. Please set AWS credentials in application.yml");
            }

            // Generate unique file name
            String uploadId = UUID.randomUUID().toString();
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String uniqueFileName = uploadId + fileExtension;

            // Build S3 key path based on upload type
            String s3Key = buildS3KeyForUpload(uploadType, userId, uniqueFileName);

            // Build the PutObjectRequest
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            // Upload to S3
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // Build and return the file URL
            String fileUrl = buildFileUrl(s3Key);

            log.info("Successfully uploaded file to S3 - userId: {}, uploadType: {}, key: {}",
                    userId, uploadType, s3Key);

            return fileUrl;

        } catch (IOException e) {
            log.error("Error reading file for upload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Build S3 key path for direct upload
     */
    private String buildS3KeyForUpload(String uploadType, Long userId, String fileName) {
        return switch (uploadType.toUpperCase()) {
            case "ARTIST_PROFILE_PHOTO" -> "profiles/artist_profile/" + userId + "/" + fileName;
            case "COVER_PHOTO" -> "cover_photo_url/" + userId + "/" + fileName;
            case "ID_PROOF" -> "id_proof_url/" + userId + "/" + fileName;
            case "DANCE_SHOWREEL" -> "dance_showreel_url/" + userId + "/" + fileName;
            case "RECRUITER_PROFILE_PHOTO" -> "profiles/recruiter_profile/" + userId + "/" + fileName;
            case "BLOG_IMAGE" -> "blogs/" + fileName;
            default -> "uploads/general/" + userId + "/" + fileName;
        };
    }
}
