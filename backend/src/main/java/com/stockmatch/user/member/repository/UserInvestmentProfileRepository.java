package com.stockmatch.user.member.repository;

import com.stockmatch.user.member.domain.UserInvestmentProfile;
import com.stockmatch.user.member.domain.enums.UserInvestmentType;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserInvestmentProfileRepository extends JpaRepository<UserInvestmentProfile, Long> {

    Optional<UserInvestmentProfile> findByUserId(Long userId);

    List<UserInvestmentProfile> findByInvestmentType(UserInvestmentType type);

    @Modifying
    @Query("DELETE FROM UserInvestmentProfile uip WHERE uip.user.id IN : userIds")
    void deleteAllByUserIds(@Param("userIds")List<Long> userIds);
}
