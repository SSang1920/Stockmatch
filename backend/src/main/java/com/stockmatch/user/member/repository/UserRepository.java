package com.stockmatch.user.member.repository;

import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.member.domain.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    //탈퇴한 지 30일 지났고, 상태가 DELETED인 유저들 조회
    @Query("SELECT u FROM User u WHERE u.status = 'DELETED' And u.deletedAt <:thresholdDate")
    List<User> findUsersToBeDeleted(@Param("thresholdDate")LocalDateTime thresholdDate);

    //유저들 한방에 삭제
    @Modifying
    @Query("DELETE FROM User u WHERE u.id IN : userIds")
    void deleteAllByIds(@Param("userIds") List<Long> userIds);

    long countByLastLoginAtBetween(LocalDateTime start, LocalDateTime end);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByDeletedAtBetween(LocalDateTime start, LocalDateTime end);
}
