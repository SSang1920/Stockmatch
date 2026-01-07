package com.stockmatch.stock.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.domain.DailyPrice;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.dto.DailyPriceResponse;
import com.stockmatch.stock.repository.DailyPriceRepository;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPriceServiceImpl implements DailyPriceService {

    private final DailyPriceRepository dailyPriceRepository;
    private final SecurityRepository securityRepository;
    private final DailyPriceSyncService dailyPriceSyncService;

    /**
     * 일반 조회용
     * DB에 없거나 부족한 구간이 있으면 자동으로 KIS API 호출
     */
    @Override
    public List<DailyPriceResponse> getDailyPrices(String ticker, LocalDate from, LocalDate to) {

        // 날짜 검증
        validateDateRange(from, to);

        // 종목 존재 여부 확인
        Security security = securityRepository.findByTicker(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        try {
            syncMissingData(security, from, to);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to sync daily prices for ticker: {}", ticker, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

        // 최종 엔티티 조회
        List<DailyPrice> prices = dailyPriceRepository.findBySecurityIdAndDateBetweenOrderByDateAsc(
                security.getId(), from, to
        );

        // 엔티티 -> DTO 변환
        return prices.stream()
                .map(DailyPriceResponse::from)
                .toList();
    }

    /**
     * 관리자용 강제 동기화
     */
    @Override
    @Transactional
    public void syncDailyPrices(String ticker, LocalDate from, LocalDate to) {
        // 날짜 검증
        validateDateRange(from, to);

        // 종목 조회
        Security security = securityRepository.findByTicker(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        try {
            // 동기화
            dailyPriceSyncService.syncRange(security, from, to);
        } catch (Exception e) {
            log.error("Failed to sync daily prices for ticker: {}", ticker, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * 부족한 구간 동기화 로직 분리
     */
    private void syncMissingData(Security security, LocalDate from, LocalDate to) {
        // 이 종목의 마지막 저장된 일자/첫번째로 저장된 일자
        LocalDate lastDate = dailyPriceRepository.findMaxDateBySecurityId(security.getId());
        LocalDate firstDate = dailyPriceRepository.findMinDateBySecurityId(security.getId());

        // 최신 구간 채우기
        if (lastDate == null || lastDate.isBefore(from)) {
            // 데이터가 없거나, 마지막 날짜가 from 이전일 경우
            dailyPriceSyncService.syncRange(security, from, to);
        } else if (lastDate.isBefore(to)) {
            LocalDate syncFrom = lastDate.plusDays(1);
            dailyPriceSyncService.syncRange(security, syncFrom, to);
        }

        // 과거 구간 채우기
        if (firstDate == null || firstDate.isAfter(from)) {
            LocalDate missingTo = (firstDate != null) ? firstDate.minusDays(1) : to;
            if (!missingTo.isBefore(from)) {
                dailyPriceSyncService.syncRange(security, from, missingTo);
            }
        }
    }

    /**
     * 날짜 범위 검증 (null, 순서, 최대 1년)
     */
    private void validateDateRange(LocalDate from, LocalDate to) {

        // null, 순서 확인
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        // from 기준 1년 초과하면 예외처리
        if (from.plusYears(1).isBefore(to)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
