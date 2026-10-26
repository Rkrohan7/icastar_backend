package com.icastar.platform.dto.recruiter;

import lombok.Data;

@Data
public class UpdateRecruiterProfileDto {

    // Company details
    private String companyName;
    private String companyDescription;
    private String companyWebsite;
    private String companyLogoUrl;
    private String profilePhotoUrl;
    private String faceVerificationUrl;
    private String idProofUrl;
    private String industry;
    private String companySize;

    // Contact person details
    private String contactPersonName;
    private String contactPhone;
    private String designation;

    // Location
    private String location;

    // Category
    private Long recruiterCategoryId;
    private String recruiterType;
}
