package com.icastar.platform.service;

import com.icastar.platform.dto.auth.EmailRegisterRequestDto;
import com.icastar.platform.entity.AuthVerificationChallenge;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthVerificationServicePasswordPolicyTest {

    private AuthVerificationChallengeRepository challengeRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthVerificationService service;

    @BeforeEach
    void setUp() {
        challengeRepository = mock(AuthVerificationChallengeRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new AuthVerificationService(
                challengeRepository,
                userRepository,
                passwordEncoder,
                mock(EmailService.class),
                mock(SmsService.class),
                mock(RecruiterProfileRepository.class),
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
                () -> service.verifyRegistration("challenge", "123456", "654321"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(challengeRepository).save(challenge);
        verifyNoInteractions(userRepository);
    }
}
