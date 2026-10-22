package com.icastar.platform.dto.superadmin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FaceVerificationQueueItemDto {
    Long id;
    String firstName;
    String lastName;
    String email;
    String stageName;
    String profileImage;
    String faceVerificationUrl;
    String faceVerificationStatus;
    Boolean isFaceVerified;
    String idProofUrl;
    Boolean idProofVerified;
    String idProofVerificationStatus;
}
