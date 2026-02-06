package com.stockmatch.admin.service;

import com.stockmatch.admin.dto.AdminDashboardResponseDto;
import com.stockmatch.portfolio.repository.PortfolioRepository;
import com.stockmatch.user.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;

    // API 에러 카운터 , Atomic을 사용하여 동시에 에러가 발생해도 다 카운트가능하게 설정
    public static final AtomicLong kisApiErrorCounter = new AtomicLong(0);

    public AdminDashboardResponseDto getDailyStatistics() {

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);

        return AdminDashboardResponseDto.builder()
                .dailyActiveUsers(userRepository.countByLastLoginAtBetween(startOfDay, endOfDay))
                .newJoinCount(userRepository.countByCreatedAtBetween(startOfDay,endOfDay))
                .withdrawnCount(userRepository.countByDeletedAtBetween(startOfDay, endOfDay))
                .dailyActivePortfolios(portfolioRepository.countTodayActivePortfolios(startOfDay,endOfDay))
                .kisApiErrorCount(kisApiErrorCounter.get())
                .build();
    }
}
