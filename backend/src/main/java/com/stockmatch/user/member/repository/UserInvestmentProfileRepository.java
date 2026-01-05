package com.stockmatch.user.member.repository;

import com.stockmatch.user.member.domain.UserInvestmentProfile;
import com.stockmatch.user.member.domain.enums.UserInvestmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserInvestmentProfileRepository extends JpaRepository<UserInvestmentProfile, Long> {

    Optional<UserInvestmentProfile> findByUserId(Long userId);

    List<UserInvestmentProfile> findByInvestmentType(UserInvestmentType type);
}
