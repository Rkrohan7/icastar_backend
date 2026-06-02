package com.icastar.platform.dto.superadmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminReportContentDto {

    private Long id;
    private String reportType;
    private String entityType;
    private Long entityId;
    private String reason;
    private String description;
    private List<String> evidenceUrls;
    private String status;
    private String priority;
    private String resolutionNotes;
    private String actionTaken;

    // Reporter details
    private Long reporterId;
    private String reporterName;
    private String reporterEmail;

    // Reported user details
    private Long reportedUserId;
    private String reportedUserName;
    private String reportedUserEmail;
    private String reportedUserRole;

    // Reviewer details
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateReportRequest {
        @NotNull
        private String reportType;
        private String entityType;
        private Long entityId;
        @NotBlank
        private String reason;
        private String description;
        private List<String> evidenceUrls;
        private Long reportedUserId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewReportRequest {
        @NotBlank
        private String status;
        private String priority;
        private String resolutionNotes;
        private String actionTaken;
    }
}
