package com.icastar.platform.service;

import com.icastar.platform.dto.auth.EmailRegisterRequestDto;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.ArtistType;
import com.icastar.platform.entity.AuthVerificationChallenge;
import com.icastar.platform.entity.RecruiterProfile;
import com.icastar.platform.entity.User;
import com.icastar.platform.exception.AuthFlowException;
import com.icastar.platform.repository.ArtistProfileRepository;
import com.icastar.platform.repository.ArtistTypeRepository;
import com.icastar.platform.repository.AuthVerificationChallengeRepository;
import com.icastar.platform.repository.RecruiterProfileRepository;
import com.icastar.platform.repository.UserRepository;
import com.icastar.platform.util.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthVerificationService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_RESENDS = 5;
    private static final int MAX_CHALLENGES_PER_WINDOW = 5;
    private static final int RATE_WINDOW_MINUTES = 15;
    private static final int RESEND_COOLDOWN_SECONDS = 30;

    private final AuthVerificationChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistTypeRepository artistTypeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${icastar.otp.expiration-minutes:5}")
    private int otpExpirationMinutes;

    @Value("${icastar.otp.length:6}")
    private int otpLength;

    @Value("${icastar.legal.terms-version:2025-12}")
    private String termsVersion;

    @Transactional(noRollbackFor = AuthFlowException.class)
    public String requestRegistration(EmailRegisterRequestDto request) {
        if (!PasswordPolicy.isValid(request.getPassword())) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, PasswordPolicy.MESSAGE);
        }

        String email = normalizeEmail(request.getEmail());
        String mobile = normalizeMobile(request.getMobile());
        if (userRepository.findByEmail(email).isPresent() || userRepository.findByMobile(mobile).isPresent()) {
            throw new AuthFlowException(HttpStatus.CONFLICT, "An account already exists with this email or mobile.");
        }

        User.UserRole role;
        try {
            role = request.getUserRole();
        } catch (RuntimeException e) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, "Invalid role.");
        }
        if (role == User.UserRole.ADMIN) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, "Invalid role.");
        }

        enforceRequestRateLimit(email, mobile, AuthVerificationChallenge.Intent.REGISTER);
        String emailOtp = generateOtp();
        String mobileOtp = generateOtp();
        AuthVerificationChallenge challenge = new AuthVerificationChallenge();
        challenge.setChallengeId(UUID.randomUUID().toString());
        challenge.setIntent(AuthVerificationChallenge.Intent.REGISTER);
        challenge.setEmail(email);
        challenge.setMobile(mobile);
        challenge.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        challenge.setPasswordPolicyValidated(true);
        challenge.setFirstName(request.getFirstName().trim());
        challenge.setLastName(request.getLastName().trim());
        challenge.setRole(role);
        challenge.setAcceptedTerms(Boolean.TRUE.equals(request.getAcceptedTerms()));
        challenge.setTermsVersion(termsVersion);
        challenge.setEmailOtpHash(passwordEncoder.encode(emailOtp));
        challenge.setMobileOtpHash(passwordEncoder.encode(mobileOtp));
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        challenge.setAttempts(0);
        challenge.setResendCount(0);

        challengeRepository.save(challenge);
        try {
            emailService.sendAuthenticationOtpEmail(email, emailOtp);
            smsService.sendAuthenticationOtpSms(mobile, mobileOtp);
        } catch (RuntimeException e) {
            challenge.setConsumedAt(LocalDateTime.now());
            challengeRepository.save(challenge);
            throw new AuthFlowException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to send verification codes. Please try again.");
        }
        return challenge.getChallengeId();
    }

    @Transactional
    public Optional<User> verifyRegistration(String challengeId, String emailOtp, String mobileOtp) {
        AuthVerificationChallenge challenge = findUsableChallenge(
                challengeId, AuthVerificationChallenge.Intent.REGISTER);
        if (!incrementAttemptOrClose(challenge)) {
            challengeRepository.save(challenge);
            return Optional.empty();
        }

        boolean emailValid = passwordEncoder.matches(emailOtp, challenge.getEmailOtpHash());
        boolean mobileValid = passwordEncoder.matches(mobileOtp, challenge.getMobileOtpHash());
        if (!emailValid || !mobileValid) {
            closeIfAttemptsExhausted(challenge);
            challengeRepository.save(challenge);
            return Optional.empty();
        }

        if (!challenge.isPasswordPolicyValidated()) {
            challenge.setConsumedAt(LocalDateTime.now());
            challengeRepository.save(challenge);
            throw new AuthFlowException(HttpStatus.BAD_REQUEST,
                    PasswordPolicy.MESSAGE + " Please start registration again.");
        }

        if (userRepository.findByEmail(challenge.getEmail()).isPresent()
                || userRepository.findByMobile(challenge.getMobile()).isPresent()) {
            challenge.setConsumedAt(LocalDateTime.now());
            challengeRepository.save(challenge);
            throw new AuthFlowException(HttpStatus.CONFLICT, "An account already exists with this email or mobile.");
        }

        User user = new User();
        user.setEmail(challenge.getEmail());
        user.setMobile(challenge.getMobile());
        user.setPassword(challenge.getPasswordHash());
        user.setRole(challenge.getRole());
        user.setStatus(User.UserStatus.ACTIVE);
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        user.setIsVerified(true);
        user.setEmailVerified(true);
        user.setMobileVerified(true);
        user.setFailedLoginAttempts(0);
        user.setFirstName(challenge.getFirstName());
        user.setLastName(challenge.getLastName());
        user.setTermsAcceptedAt(LocalDateTime.now());
        user.setTermsVersion(challenge.getTermsVersion());
        User savedUser = userRepository.save(user);

        createRoleProfile(savedUser);
        challenge.setConsumedAt(LocalDateTime.now());
        challengeRepository.save(challenge);
        return Optional.of(savedUser);
    }

    @Transactional(noRollbackFor = AuthFlowException.class)
    public String requestLogin(String requestedEmail, String password) {
        String email = normalizeEmail(requestedEmail);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthFlowException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new AuthFlowException(HttpStatus.FORBIDDEN, "Account is not active. Please contact support.");
        }
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "Account is temporarily locked. Please try again later.");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            int failedAttempts = user.getFailedLoginAttempts() == null ? 1 : user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= 5) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
            }
            userRepository.save(user);
            throw new AuthFlowException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        enforceRequestRateLimit(email, user.getMobile(), AuthVerificationChallenge.Intent.LOGIN);
        String otp = generateOtp();
        AuthVerificationChallenge challenge = new AuthVerificationChallenge();
        challenge.setChallengeId(UUID.randomUUID().toString());
        challenge.setIntent(AuthVerificationChallenge.Intent.LOGIN);
        challenge.setEmail(email);
        challenge.setMobile(user.getMobile());
        challenge.setEmailOtpHash(passwordEncoder.encode(otp));
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        challenge.setAttempts(0);
        challenge.setResendCount(0);
        challengeRepository.save(challenge);
        try {
            emailService.sendAuthenticationOtpEmail(email, otp);
        } catch (RuntimeException e) {
            challenge.setConsumedAt(LocalDateTime.now());
            challengeRepository.save(challenge);
            throw new AuthFlowException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to send login verification code. Please try again.");
        }
        return challenge.getChallengeId();
    }

    @Transactional(noRollbackFor = AuthFlowException.class)
    public Optional<User> verifyLogin(String challengeId, String otp) {
        AuthVerificationChallenge challenge = findUsableChallenge(
                challengeId, AuthVerificationChallenge.Intent.LOGIN);
        if (!incrementAttemptOrClose(challenge)) {
            challengeRepository.save(challenge);
            return Optional.empty();
        }
        if (!passwordEncoder.matches(otp, challenge.getEmailOtpHash())) {
            closeIfAttemptsExhausted(challenge);
            challengeRepository.save(challenge);
            return Optional.empty();
        }

        User user = userRepository.findByEmail(challenge.getEmail())
                .orElseThrow(() -> new AuthFlowException(HttpStatus.UNAUTHORIZED, "Invalid login challenge."));
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            challenge.setConsumedAt(LocalDateTime.now());
            challengeRepository.save(challenge);
            throw new AuthFlowException(HttpStatus.FORBIDDEN, "Account is not active. Please contact support.");
        }

        challenge.setConsumedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        challengeRepository.save(challenge);
        return Optional.of(userRepository.save(user));
    }

    @Transactional(noRollbackFor = AuthFlowException.class)
    public void resendRegistration(String challengeId, String channel) {
        AuthVerificationChallenge challenge = findUsableChallenge(
                challengeId, AuthVerificationChallenge.Intent.REGISTER);
        checkResendLimits(challenge);
        String otp = generateOtp();
        challenge.setResendCount(challenge.getResendCount() + 1);
        challenge.setLastResendAt(LocalDateTime.now());
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        if ("email".equalsIgnoreCase(channel)) {
            challenge.setEmailOtpHash(passwordEncoder.encode(otp));
        } else if ("mobile".equalsIgnoreCase(channel)) {
            challenge.setMobileOtpHash(passwordEncoder.encode(otp));
        } else {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, "Channel must be email or mobile.");
        }
        challengeRepository.save(challenge);
        try {
            if ("email".equalsIgnoreCase(channel)) {
                emailService.sendAuthenticationOtpEmail(challenge.getEmail(), otp);
            } else {
                smsService.sendAuthenticationOtpSms(challenge.getMobile(), otp);
            }
        } catch (RuntimeException e) {
            throw new AuthFlowException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to resend verification code. Please try again.");
        }
    }

    @Transactional(noRollbackFor = AuthFlowException.class)
    public void resendLogin(String challengeId) {
        AuthVerificationChallenge challenge = findUsableChallenge(
                challengeId, AuthVerificationChallenge.Intent.LOGIN);
        checkResendLimits(challenge);
        String otp = generateOtp();
        challenge.setEmailOtpHash(passwordEncoder.encode(otp));
        challenge.setResendCount(challenge.getResendCount() + 1);
        challenge.setLastResendAt(LocalDateTime.now());
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        challengeRepository.save(challenge);
        try {
            emailService.sendAuthenticationOtpEmail(challenge.getEmail(), otp);
        } catch (RuntimeException e) {
            throw new AuthFlowException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to resend login verification code. Please try again.");
        }
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void purgeExpiredChallenges() {
        challengeRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private void createRoleProfile(User user) {
        if (user.getRole() == User.UserRole.RECRUITER) {
            RecruiterProfile profile = new RecruiterProfile();
            profile.setUser(user);
            profile.setIsVerifiedCompany(false);
            profile.setCompanyName("");
            profile.setContactPersonName(user.getFirstName() + " " + user.getLastName());
            recruiterProfileRepository.save(profile);
            return;
        }

        ArtistType defaultType = artistTypeRepository.findByName("OTHER")
                .orElseGet(() -> artistTypeRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new AuthFlowException(
                                HttpStatus.INTERNAL_SERVER_ERROR, "Artist types are not configured.")));
        ArtistProfile profile = new ArtistProfile();
        profile.setUser(user);
        profile.setArtistType(defaultType);
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setIsVerifiedBadge(false);
        profile.setIsProfileComplete(false);
        profile.setTotalApplications(0);
        profile.setSuccessfulHires(0);
        artistProfileRepository.save(profile);
    }

    private void enforceRequestRateLimit(
            String email, String mobile, AuthVerificationChallenge.Intent intent) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RATE_WINDOW_MINUTES);
        if (challengeRepository.countByEmailAndIntentAndCreatedAtAfter(email, intent, windowStart)
                >= MAX_CHALLENGES_PER_WINDOW
                || (mobile != null && challengeRepository.countByMobileAndIntentAndCreatedAtAfter(
                        mobile, intent, windowStart) >= MAX_CHALLENGES_PER_WINDOW)) {
            throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "Too many verification requests. Try again later.");
        }
    }

    private AuthVerificationChallenge findUsableChallenge(String challengeId, AuthVerificationChallenge.Intent intent) {
        AuthVerificationChallenge challenge = challengeRepository.findByChallengeIdForUpdate(challengeId)
                .orElseThrow(() -> new AuthFlowException(HttpStatus.BAD_REQUEST, "Invalid or expired verification challenge."));
        if (challenge.getIntent() != intent
                || challenge.getConsumedAt() != null
                || !challenge.getExpiresAt().isAfter(LocalDateTime.now())
                || challenge.getAttempts() >= MAX_ATTEMPTS) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, "Invalid or expired verification challenge.");
        }
        return challenge;
    }

    private boolean incrementAttemptOrClose(AuthVerificationChallenge challenge) {
        challenge.setAttempts(challenge.getAttempts() + 1);
        return challenge.getAttempts() <= MAX_ATTEMPTS;
    }

    private void closeIfAttemptsExhausted(AuthVerificationChallenge challenge) {
        if (challenge.getAttempts() >= MAX_ATTEMPTS) {
            challenge.setConsumedAt(LocalDateTime.now());
        }
    }

    private void checkResendLimits(AuthVerificationChallenge challenge) {
        if (challenge.getResendCount() >= MAX_RESENDS) {
            throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "Resend limit reached. Start again later.");
        }
        if (challenge.getLastResendAt() != null
                && challenge.getLastResendAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another OTP.");
        }
    }

    private String generateOtp() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength);
        return Integer.toString(secureRandom.nextInt(max - min) + min);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMobile(String mobile) {
        String normalized = mobile.replaceAll("\\D", "");
        if (normalized.startsWith("91") && normalized.length() == 12) {
            normalized = normalized.substring(2);
        }
        if (!normalized.matches("^[6-9]\\d{9}$")) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, "Mobile number must be a valid 10-digit Indian number.");
        }
        return normalized;
    }
}
