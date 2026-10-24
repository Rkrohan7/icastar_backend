package com.icastar.platform.dto.recruiter;

import lombok.Data;
import java.util.List;

@Data
public class RecruiterProfileDto {
    private Long id;

    // === USER DETAILS ===
    private Long userId;
    private String email;
    private String mobile;
    private String firstName;
    private String lastName;
    private Boolean isVerified;
    private Integer profileCompletionPercentage;
    private String faceVerificationUrl;
    private String faceVerificationStatus;
    private Boolean isFaceVerified;
    private String idProofUrl;
    private String idProofVerificationStatus;
    private Boolean idProofVerified;

    // === COMPANY DETAILS ===
    private String companyName;
    private String contactPersonName;
    private String contactPhone;
    private String designation;
    private String companyDescription;
    private String companyWebsite;
    private String companyLogoUrl;
    private String profilePhotoUrl;
    private String industry;
    private String companySize;
    private String location;
    private Boolean isVerifiedCompany;

    // === METRICS / STATS ===
    private Integer totalJobsPosted;
    private Integer successfulHires;
    private Integer chatCredits;

    // === CATEGORY INFO ===
    private Long recruiterCategoryId;
    private String recruiterCategoryName;


    // === COMPUTED FIELDS ===
    public String getFullName() {
        String f = (firstName != null) ? firstName : "";
        String l = (lastName != null) ? lastName : "";
        return (f + " " + l).trim();
    }
}
