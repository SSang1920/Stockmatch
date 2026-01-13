package com.stockmatch.user.member.service;

import com.stockmatch.portfolio.repository.PortfolioRepository;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.member.repository.AlphaVantageKeyRepository;
import com.stockmatch.user.member.repository.UserInvestmentProfileRepository;
import com.stockmatch.user.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupService {
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final AlphaVantageKeyRepository alphaVantageKeyRepository;
    private final UserInvestmentProfileRepository userInvestmentProfileRepository;

    @Scheduled(cron = " 0 0 3 * * *")
    @Transactional
    public void cleanupDeletedUsers() {
        log.info("===탈퇴 회원 영구 삭제 스케줄러 시작 ===");

        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(30);

        List<User> targets = userRepository.findUsersToBeDeleted(thresholdDate);

        if(targets.isEmpty()) {
            log.info("삭제할 대상이 없습니다.");
            return;
        }

        List<Long> targetIds = targets.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        int count = targetIds.size();
        log.info("삭제 대상 유저 수: {}명", count);

        try {
            portfolioRepository.deleteAllByUserIds(targetIds);
            alphaVantageKeyRepository.deleteAllByUserIds(targetIds);
            userInvestmentProfileRepository.deleteAllByUserIds(targetIds);

            userRepository.deleteAllByIds(targetIds);

            log.info("=== {}명의 탈퇴 회원 데이터 영구 삭제 완료 ===", count);
        } catch (Exception e) {
            log.error("영구 삭제 처리 중 오류 발생", e);

        }
    }
}
