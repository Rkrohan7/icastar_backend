package com.icastar.platform.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class AuthVerificationDtos {

    private AuthVerificationDtos() {
    }

    @Data
    public static class RegistrationVerifyRequest {
        @NotBlank
        private String challengeId;
        @NotBlank
        @Size(min = 4, max = 10)
        private String emailOtp;
        @NotBlank
        @Size(min = 4, max = 10)
        private String mobileOtp;
    }

    @Data
    public static class RegistrationResendRequest {
        @NotBlank
        private String challengeId;
        @NotBlank
        @Pattern(regexp = "email|mobile", flags = Pattern.Flag.CASE_INSENSITIVE)
        private String channel;
    }

    @Data
    public static class LoginOtpRequest {
        @NotBlank
        @Email
        private String email;
        @NotBlank
        @Size(min = 6)
        private String password;
    }

    @Data
    public static class LoginVerifyRequest {
        @NotBlank
        private String challengeId;
        @NotBlank
        @Size(min = 4, max = 10)
        private String otp;
    }

    @Data
    public static class LoginResendRequest {
        @NotBlank
        private String challengeId;
    }
}
