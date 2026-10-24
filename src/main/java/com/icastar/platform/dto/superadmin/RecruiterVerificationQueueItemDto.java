package com.icastar.platform.dto.superadmin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RecruiterVerificationQueueItemDto {
    Long id;
    Long userId;
    String firstName;
    String lastName;
    String email;
    String companyName;
    String contactPersonName;
    String faceVerificationUrl;
    String faceVerificationStatus;
    Boolean isFaceVerified;
    String idProofUrl;
    String idProofVerificationStatus;
    Boolean idProofVerified;
    Boolean isVerified;
}
