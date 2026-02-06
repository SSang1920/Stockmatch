package com.stockmatch.stock.service;

import com.stockmatch.admin.service.AdminDashboardService;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.client.kis.KisTrendClient;
import com.stockmatch.stock.client.kis.KisVolumeClient;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.dto.StockTrendResponse;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DomesticTrendService {

    private final KisVolumeClient kisVolumeClient;
    private final KisTrendClient kisTrendClient;
    private final SecurityRepository securityRepository;

    /**
     * 거래량 순위 조회
     */
    public List<StockTrendResponse> getMostActive() {
        // API 호출
        List<KisVolumeClient.KisVolumeItem> rawData;
        try {
            rawData = kisVolumeClient.getDomesticVolumeRank();
        } catch (Exception e) {
            AdminDashboardService.kisApiErrorCounter.incrementAndGet();

            // API 호출 자체 실패 시
            log.error("Failed to fetch domestic volume rank", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

        // 데이터가 비어있을 경우 예외 처리
        if (rawData == null || rawData.isEmpty()) {
            throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
        }

        // DB 병합 및 DTO 변환
        return mergeVolumeData(rawData);
    }

    /**
     * 급등 순위 조회
     */
    public List<StockTrendResponse> getGainers() {
        // API 호출
        List<KisTrendClient.KisTrendRankItem> rawData;
        try {
            rawData = kisTrendClient.getDomesticGainers();
        } catch (Exception e) {
            AdminDashboardService.kisApiErrorCounter.incrementAndGet();
            // API 호출 자체 실패 시
            log.error("Failed to fetch domestic gainers rank", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

        // 데이터가 비어있을 경우 예외 처리
        if (rawData == null || rawData.isEmpty()) {
            throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
        }

        // DB 병합 및 DTO 변환
        return mergeTrendData(rawData);
    }

    /**
     * 급락 순위 조회
     */
    public List<StockTrendResponse> getLosers() {
        // API 호출
        List<KisTrendClient.KisTrendRankItem> rawData;
        try {
            rawData = kisTrendClient.getDomesticLosers();
        } catch (Exception e) {
            AdminDashboardService.kisApiErrorCounter.incrementAndGet();
            // API 호출 자체 실패 시
            log.error("Failed to fetch domestic losers rank", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

        // 데이터가 비어있을 경우 예외 처리
        if (rawData == null || rawData.isEmpty()) {
            throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
        }

        // DB 병합 및 DTO 변환
        return mergeTrendData(rawData);
    }

    // ===== 거래량 로직 =====

    /**
     * API 데이터 + DB 데이터 병합 로직
     */
    private List<StockTrendResponse> mergeVolumeData(List<KisVolumeClient.KisVolumeItem> apiItems) {
        if (apiItems == null || apiItems.isEmpty()) {
            return Collections.emptyList();
        }

        // 티커 추출
        List<String> tickers = apiItems.stream()
                .limit(10)
                .map(KisVolumeClient.KisVolumeItem::getMkscShrnIscd)
                .toList();

        // DB Bulk 조회
        Map<String, Security> securityMap = getSecurityMap(tickers);

        // 변환 및 합치기
        return apiItems.stream()
                .limit(10)
                .map(item -> {
                    String ticker = item.getMkscShrnIscd();
                    Security dbSecurity = securityMap.get(ticker);

                    String finalName = (dbSecurity != null) ? dbSecurity.getName() : item.getHtsKorIsnm();

                    return mapToDto(item, finalName);
                })
                .collect(Collectors.toList());
    }

    /**
     * DTO 변환 헬퍼
     */
    private StockTrendResponse mapToDto(KisVolumeClient.KisVolumeItem item, String name) {
        try {
            long currentPrice = Long.parseLong(item.getStckPrpr().replace(",", ""));
            long changeValue = Long.parseLong(item.getPrdyVrss().replace(",", ""));
            double changeRate = Double.parseDouble(item.getPrdyCtrt().replace(",", ""));

            String priceStr = String.format("%,d원", currentPrice);

            // 부호 처리
            String sign = changeValue > 0 ? "+" : (changeValue < 0 ? "-" : "");
            String changeStr = sign + String.format("%,d", Math.abs(changeValue));
            String rateStr = (changeRate > 0 ? "+" : "") + String.format("%.2f%%", changeRate);

            return new StockTrendResponse(
                    item.getMkscShrnIscd(),
                    name,
                    priceStr,
                    changeStr,
                    rateStr,
                    "KR"
            );
        } catch (Exception e) {
            log.warn("Error parsing rank item: {}", item.getMkscShrnIscd());
            throw new BusinessException(ErrorCode.EXTERNAL_API_PARSING_ERROR);
        }
    }

    // ===== 등락률 로직 =====
    /**
     * API 데이터 + DB 데이터 병합 로직
     */
    private List<StockTrendResponse> mergeTrendData(List<KisTrendClient.KisTrendRankItem> apiItems) {
        if (apiItems == null || apiItems.isEmpty()) {
            return Collections.emptyList();
        }

        // 티커 추출
        List<String> tickers = apiItems.stream()
                .limit(10)
                .map(KisTrendClient.KisTrendRankItem::getTicker)
                .toList();

        // DB Bulk 조회
        Map<String, Security> securityMap = getSecurityMap(tickers);

        // 변환 및 합치기
        return apiItems.stream()
                .limit(10)
                .map(item -> {
                    String ticker = item.getTicker();
                    Security dbSecurity = securityMap.get(ticker);

                    String finalName = (dbSecurity != null) ? dbSecurity.getName() : item.getName();

                    return mapTrendToDto(item, finalName);
                })
                .collect(Collectors.toList());
    }

    /**
     * DTO 변환 헬퍼
     */
    private StockTrendResponse mapTrendToDto(KisTrendClient.KisTrendRankItem item, String name) {
        try {
            long currentPrice = Long.parseLong(item.getCurrentPrice().replace(",", ""));
            long changeValue = Long.parseLong(item.getChangeAmount().replace(",", ""));
            double changeRate = Double.parseDouble(item.getChangeRate().replace(",", ""));

            String priceStr = String.format("%,d원", currentPrice);

            // 부호 처리
            String sign = changeValue > 0 ? "+" : (changeValue < 0 ? "-" : "");
            String changeStr = sign + String.format("%,d", Math.abs(changeValue));
            String rateStr = (changeRate > 0 ? "+" : "") + String.format("%.2f%%", changeRate);

            return new StockTrendResponse(
                    item.getTicker(),
                    name,
                    priceStr,
                    changeStr,
                    rateStr,
                    "KR"
            );
        } catch (Exception e) {
            log.warn("Error parsing trend rank item: {}", item.getTicker());
            throw new BusinessException(ErrorCode.EXTERNAL_API_PARSING_ERROR);
        }
    }

    // ===== 공통 유틸 =====

    /**
     * 티커 리스트로 DB Security Map 조회
     */
    private Map<String, Security> getSecurityMap(List<String> tickers) {
        List<Security> securities = securityRepository.findByTickerIn(tickers);
        return securities.stream()
                .collect(Collectors.toMap(Security::getTicker, Function.identity()));
    }

}
