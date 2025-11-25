package com.stockmatch.stock.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.domain.DailyPrice;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.dto.DailyPriceResponse;
import com.stockmatch.stock.repository.DailyPriceRepository;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPriceServiceImpl implements DailyPriceService {

    private final DailyPriceRepository dailyPriceRepository;
    private final SecurityRepository securityRepository;

    @Override
    public List<DailyPriceResponse> getDailyPrices(String ticker, LocalDate from, LocalDate to) {

        // 날짜 검증
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        // 종목 존재 여부 확인
        Security security = securityRepository.findByTicker(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        // 엔티티 조회
        List<DailyPrice> prices = dailyPriceRepository.findBySecurityTickerAndDateBetweenOrderByDateAsc(
                security.getTicker(),
                from,
                to
        );

        // 엔티티 -> DTO 변환
        return prices.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 엔티티 -> DTO 변환 메서드
     */
    private DailyPriceResponse toResponse(DailyPrice entity) {
        return new DailyPriceResponse(
                entity.getDate(),
                entity.getOpenPrice(),
                entity.getClosePrice(),
                entity.getHighPrice(),
                entity.getLowPrice(),
                entity.getVolume()
        );
    }
}
