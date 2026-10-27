package com.icastar.platform.service;

import com.icastar.platform.dto.auth.EmailRegisterRequestDto;
import com.icastar.platform.entity.AuthVerificationChallenge;
import com.icastar.platform.entity.User;
import com.icastar.platform.exception.AuthFlowException;
import com.icastar.platform.repository.ArtistProfileRepository;
import com.icastar.platform.repository.ArtistTypeRepository;
import com.icastar.platform.repository.AuthVerificationChallengeRepository;
import com.icastar.platform.repository.RecruiterProfileRepository;
import com.icastar.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthVerificationServicePasswordPolicyTest {

    private AuthVerificationChallengeRepository challengeRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private RecruiterProfileRepository recruiterProfileRepository;
    private AuthVerificationService service;

    @BeforeEach
    void setUp() {
        challengeRepository = mock(AuthVerificationChallengeRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);
        recruiterProfileRepository = mock(RecruiterProfileRepository.class);
        service = new AuthVerificationService(
                challengeRepository,
                userRepository,
                passwordEncoder,
                emailService,
                recruiterProfileRepository,
                mock(ArtistProfileRepository.class),
                mock(ArtistTypeRepository.class));
    }

    @Test
    void requestRegistrationRejectsWeakPasswordBeforeStartingVerification() {
        EmailRegisterRequestDto request = new EmailRegisterRequestDto();
        request.setPassword("weakpass1!");

        AuthFlowException exception = assertThrows(
                AuthFlowException.class, () -> service.requestRegistration(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(challengeRepository, userRepository, passwordEncoder);
    }

    @Test
    void requestRegistrationSendsOnlyEmailOtp() {
        EmailRegisterRequestDto request = new EmailRegisterRequestDto();
        request.setEmail("newuser@example.com");
        request.setMobile("9876543210");
        request.setPassword("StrongPass1!");
        request.setRole("RECRUITER");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setAcceptedTerms(true);
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByMobile("9876543210")).thenReturn(Optional.empty());
        when(challengeRepository.countByEmailAndIntentAndCreatedAtAfter(
                eq("newuser@example.com"), eq(AuthVerificationChallenge.Intent.REGISTER), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(challengeRepository.countByMobileAndIntentAndCreatedAtAfter(
                eq("9876543210"), eq(AuthVerificationChallenge.Intent.REGISTER), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-value");

        service.requestRegistration(request);

        org.mockito.ArgumentCaptor<AuthVerificationChallenge> challengeCaptor =
                org.mockito.ArgumentCaptor.forClass(AuthVerificationChallenge.class);
        verify(challengeRepository).save(challengeCaptor.capture());
        assertNull(challengeCaptor.getValue().getMobileOtpHash());
        verify(emailService).sendAuthenticationOtpEmail(eq("newuser@example.com"), anyString());
    }

    @Test
    void resendRegistrationRejectsMobileChannel() {
        AuthFlowException exception = assertThrows(
                AuthFlowException.class, () -> service.resendRegistration("challenge", "mobile"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(challengeRepository, passwordEncoder, emailService);
    }

    @Test
    void verifyRegistrationRejectsChallengeWithoutValidatedPasswordPolicy() {
        AuthVerificationChallenge challenge = new AuthVerificationChallenge();
        challenge.setChallengeId("challenge");
        challenge.setIntent(AuthVerificationChallenge.Intent.REGISTER);
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setEmailOtpHash("email-otp-hash");
        challenge.setMobileOtpHash("mobile-otp-hash");
        when(challengeRepository.findByChallengeIdForUpdate("challenge"))
                .thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        AuthFlowException exception = assertThrows(
                AuthFlowException.class,
                () -> service.verifyRegistration("challenge", "123456"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(challengeRepository).save(challenge);
        verifyNoInteractions(userRepository);
    }

    @Test
    void verifyRegistrationCreatesAccountWithMobileUnverified() {
        AuthVerificationChallenge challenge = new AuthVerificationChallenge();
        challenge.setChallengeId("challenge");
        challenge.setIntent(AuthVerificationChallenge.Intent.REGISTER);
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setEmail("newuser@example.com");
        challenge.setMobile("9876543210");
        challenge.setEmailOtpHash("email-otp-hash");
        challenge.setPasswordHash("password-hash");
        challenge.setPasswordPolicyValidated(true);
        challenge.setRole(User.UserRole.RECRUITER);
        challenge.setFirstName("Test");
        challenge.setLastName("User");
        challenge.setTermsVersion("2025-12");
        when(challengeRepository.findByChallengeIdForUpdate("challenge"))
                .thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("123456", "email-otp-hash")).thenReturn(true);
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByMobile("9876543210")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.verifyRegistration("challenge", "123456").orElseThrow();

        assertEquals(Boolean.TRUE, user.getEmailVerified());
        assertFalse(user.getMobileVerified());
        verify(recruiterProfileRepository).save(any());
    }
}
