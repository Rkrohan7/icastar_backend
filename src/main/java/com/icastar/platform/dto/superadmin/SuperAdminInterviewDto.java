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
public class SuperAdminInterviewDto {

    private Long applicationId;
    private LocalDateTime interviewScheduledAt;
    private String interviewNotes;
    private String status;

    // Job details
    private Long jobId;
    private String jobTitle;
    private String jobLocation;

    // Artist details
    private Long artistId;
    private String artistName;
    private String artistEmail;
    private String artistMobile;
    private String artistProfileImage;

    // Recruiter details
    private Long recruiterId;
    private String recruiterName;
    private String recruiterEmail;
    private String companyName;

    private LocalDateTime createdAt;
}
