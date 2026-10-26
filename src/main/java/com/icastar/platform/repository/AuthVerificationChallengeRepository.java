package com.icastar.platform.repository;

import com.icastar.platform.entity.AuthVerificationChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthVerificationChallengeRepository extends JpaRepository<AuthVerificationChallenge, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AuthVerificationChallenge c WHERE c.challengeId = :challengeId")
    Optional<AuthVerificationChallenge> findByChallengeIdForUpdate(
            @Param("challengeId") String challengeId);

    long countByEmailAndIntentAndCreatedAtAfter(
            String email, AuthVerificationChallenge.Intent intent, LocalDateTime createdAt);

    long countByMobileAndIntentAndCreatedAtAfter(
            String mobile, AuthVerificationChallenge.Intent intent, LocalDateTime createdAt);

    long deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
