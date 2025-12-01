package com.stockmatch.stock.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.client.ExternalDailyPriceClient;
import com.stockmatch.stock.client.kis.KisDailyPriceClient;
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
    private final KisDailyPriceClient kisDailyPriceClient;

    /**
     * 일반 조회용
     * DB에 없거나 부족한 구간이 있으면 자동으로 KIS API 호출
     */
    @Override
    @Transactional
    public List<DailyPriceResponse> getDailyPrices(String ticker, LocalDate from, LocalDate to) {

        // 날짜 검증
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        // 종목 존재 여부 확인
        Security security = securityRepository.findByTicker(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        // 이 종목의 마지막 저장된 일자
        LocalDate lastDate = dailyPriceRepository.findMaxDateBySecurityId(security.getId());

        // 데이터가 없거나, 마지막 날짜가 from 이전일 경우 -> 전체 구간 동기화
        if (lastDate == null || lastDate.isBefore(from)) {
            syncDailyPricesInternal(security, from, to);
        }

        // 일부만 있을 경우 -> LastDate+1 ~ to 구간 동기화
        else if (lastDate.isBefore(to)) {
            LocalDate syncFrom = lastDate.plusDays(1);
            syncDailyPricesInternal(security, syncFrom, to);
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

        // 종목 조회
        Security security = securityRepository.findByTicker(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        // 동기화
        syncDailyPricesInternal(security, from, to);
    }

    /**
     * 실제 동기화 공통 로직
     */
    private void syncDailyPricesInternal(Security security, LocalDate from, LocalDate to) {

        // KIS API에서 기간별 시세 가져오기
        List<ExternalDailyPriceClient.DailyPriceItem> items =
                kisDailyPriceClient.getDailyPrices(security.getTicker(), from, to);

        // daily_price upsert
        for (ExternalDailyPriceClient.DailyPriceItem item : items) {

            DailyPrice entity = dailyPriceRepository.findBySecurityIdAndDate(security.getId(), item.date())
                    .orElse(null);

            if (entity == null) {
                // 신규 생성
                entity = new DailyPrice(
                        null,
                        security,
                        item.date(),
                        item.openPrice(),
                        item.closePrice(),
                        item.highPrice(),
                        item.lowPrice(),
                        item.volume()
                );
            } else {
                // 업데이트
                entity = new DailyPrice(
                        entity.getId(),
                        security,
                        item.date(),
                        item.openPrice(),
                        item.closePrice(),
                        item.highPrice(),
                        item.lowPrice(),
                        item.volume()
                );
            }

            dailyPriceRepository.save(entity);
        }
    }
}
