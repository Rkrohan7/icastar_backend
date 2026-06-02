package com.icastar.platform.dto.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminAuditionDto {

    private Long id;
    private String title;
    private String description;
    private String auditionType;
    private String status;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private String meetingLink;
    private String instructions;
    private String feedback;
    private Integer rating;
    private String recordingUrl;
    private Boolean isOpenAudition;

    // Artist details
    private Long artistId;
    private String artistName;
    private String artistEmail;
    private String artistProfileImage;
    private String artistType;

    // Recruiter details
    private Long recruiterId;
    private String recruiterName;
    private String recruiterEmail;
    private String companyName;

    // Job Application details
    private Long jobApplicationId;
    private String jobTitle;

    // Target artist type for open auditions
    private Long targetArtistTypeId;
    private String targetArtistTypeName;

    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        private String status;
        private String feedback;
        private Integer rating;
    }
}
