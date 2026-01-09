package com.stockmatch.stock.service;

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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OverseasTrendService {

    private final KisVolumeClient kisVolumeClient;
    private final KisTrendClient kisTrendClient;
    private final SecurityRepository securityRepository;

    /**
     * 거래량 순위 조회
     */
    public List<StockTrendResponse> getMostActive() {
        // 나스닥 & 뉴욕 거래소 API 호출하여 합치기
        List<KisVolumeClient.KisOverseasVolumeItem> allItems = new ArrayList<>();

        try {
            allItems.addAll(kisVolumeClient.getOverseasVolumeRank("NAS"));
            allItems.addAll(kisVolumeClient.getOverseasVolumeRank("NYS"));
        } catch (Exception e) {
            log.error("Failed to fetch overseas volume rank", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

        // 데이터가 비어있을 경우 예외 처리
        if (allItems.isEmpty()) {
            throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
        }

        // 거래량 기준으로 내림차순 정렬
        allItems.sort((a,b) -> {
            long volA = parseLongSafe(a.getTvol());
            long volB = parseLongSafe(b.getTvol());
            return Long.compare(volB, volA);
        });

        // DB 데이터와 병합하여 DTO 반환
        return mergeVolumeData(allItems);
    }

    /**
     * 급등 순위 조회
     */
    public List<StockTrendResponse> getGainers() {
        List<KisTrendClient.KisOverseasTrendRankItem> allItems = new ArrayList<>();
        try {
            allItems.addAll(kisTrendClient.getOverseasGainers("NAS"));
            allItems.addAll(kisTrendClient.getOverseasGainers("NYS"));
        } catch (Exception e) {
            log.error("Failed to fetch overseas gainers", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

        // 데이터가 비어있을 경우 예외처리
        if (allItems.isEmpty()) {
            throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
        }

        // 상승률 내림차순 정렬
        allItems.sort((a, b) -> {
            double rateA = parseDoubleSafe(a.getChangeRate());
            double rateB = parseDoubleSafe(b.getChangeRate());
            return Double.compare(rateB, rateA);
        });

        return mergeTrendData(allItems);
    }

    /**
     * 급락 순위 조회
     */
    public List<StockTrendResponse> getLosers() {
        List<KisTrendClient.KisOverseasTrendRankItem> allItems = new ArrayList<>();
        try {
            allItems.addAll(kisTrendClient.getOverseasLosers("NAS"));
            allItems.addAll(kisTrendClient.getOverseasLosers("NYS"));
        } catch (Exception e) {
            log.error("Failed to fetch overseas losers", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

        // 데이터가 비어있을 경우 예외처리
        if (allItems.isEmpty()) {
            throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
        }

        // 하락률 오름차순 정렬
        allItems.sort((a, b) -> {
            double rateA = parseDoubleSafe(a.getChangeRate());
            double rateB = parseDoubleSafe(b.getChangeRate());
            return Double.compare(rateA, rateB);
        });

        return mergeTrendData(allItems);
    }

    // ===== 거래량 로직 =====
    /**
     * API 데이터 + DB 데이터 병합 로직
     */
    private List<StockTrendResponse> mergeVolumeData(List<KisVolumeClient.KisOverseasVolumeItem> apiItems) {
        // 티커 목록 추출
        List<String> tickers = apiItems.stream()
                .limit(10)
                .map(item -> parseTicker(item.getRsym()))
                .toList();

        // DB에서 티커로 조회
        List<Security> securities = securityRepository.findByTickerIn(tickers);
        Map<String, Security> securityMap = securities.stream()
                .collect(Collectors.toMap(Security::getTicker, Function.identity()));

        // 매핑
        return apiItems.stream()
                .limit(10)
                .map(item -> {
                    String cleanTicker = parseTicker(item.getRsym());
                    Security dbSecurity = securityMap.get(cleanTicker);

                    String finalName;
                    if (dbSecurity != null) {
                        finalName = dbSecurity.getName();
                    } else {
                        finalName = (item.getEname() != null && !item.getEname().isEmpty())
                                ? item.getEname() : item.getName();
                    }

                    return mapVolumeToDto(item, finalName, cleanTicker);
                })
                .collect(Collectors.toList());
    }

    /**
     * DTO 변환 헬퍼
     */
    private StockTrendResponse mapVolumeToDto(KisVolumeClient.KisOverseasVolumeItem item, String name, String ticker) {
        try {
            double price = Double.parseDouble(item.getLast());
            double change = Double.parseDouble(item.getDiff());
            double rate = Double.parseDouble(item.getRate());

            String priceStr = "$" + String.format("%.2f", price);
            String sign = change > 0 ? "+" : "";
            String changeStr = sign + String.format("%.2f", change);
            String rateStr = (rate > 0 ? "+" : "") + String.format("%.2f%%", rate);

            return new StockTrendResponse(
                    ticker,
                    name,
                    priceStr,
                    changeStr,
                    rateStr,
                    "US"
            );
        } catch (Exception e) {
            log.warn("Overseas DTO parse error: {}", item.getRsym());
            throw new BusinessException(ErrorCode.EXTERNAL_API_PARSING_ERROR);
        }
    }

    // ===== 등락률 로직 =====
    /**
     * API 데이터 + DB 데이터 병합 로직
     */
    private List<StockTrendResponse> mergeTrendData(List<KisTrendClient.KisOverseasTrendRankItem> apiItems) {
        // 티커 목록 추출
        List<String> tickers = apiItems.stream()
                .limit(10)
                .map(item -> parseTicker(item.getTicker()))
                .toList();

        // DB Bulk 조회
        Map<String, Security> securityMap = getSecurityMap(tickers);

        // 변환 및 합치기
        return apiItems.stream()
                .limit(10)
                .map(item -> {
                    String cleanTicker = parseTicker(item.getTicker());
                    Security dbSecurity = securityMap.get(cleanTicker);

                    String finalName = (dbSecurity != null) ? dbSecurity.getName() : item.getName();

                    return mapTrendToDto(item, finalName, cleanTicker);
                })
                .collect(Collectors.toList());
    }

    /**
     * DTO 변환 헬퍼
     */
    private StockTrendResponse mapTrendToDto(KisTrendClient.KisOverseasTrendRankItem item, String name, String ticker) {
        try {
            double price = Double.parseDouble(item.getCurrentPrice());
            double change = Double.parseDouble(item.getChangeAmount());
            double rate = Double.parseDouble(item.getChangeRate());

            String priceStr = "$" + String.format("%,.2f", price);
            String sign = change > 0 ? "+" : (change < 0 ? "-" : "");
            String changeStr = sign + String.format("%,.2f", Math.abs(change));
            String rateStr = (rate > 0 ? "+" : "") + String.format("%.2f%%", rate);

            return new StockTrendResponse(
                    ticker,
                    name,
                    priceStr,
                    changeStr,
                    rateStr,
                    "US"
            );

        } catch (Exception e) {
            log.warn("Error parsing overseas trend item: {}", item.getTicker());
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

    /**
     * 티커 정제 헬퍼 메서드 (DNAS, DNYS 제거)
     */
    private String parseTicker(String rsym) {
        if (rsym == null) return "";
        // 4자리 이상이고 특정 접두사가 있으면 자름
        if (rsym.length() > 4 && (rsym.startsWith("DNAS") || rsym.startsWith("DNYS"))) {
            return rsym.substring(4);
        }
        return rsym;
    }

    /**
     * 파싱
     */
    private long parseLongSafe(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    private double parseDoubleSafe(String value) {
        try {
            if (value == null) return 0.0;
            return Double.parseDouble(value.replace("%", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }
}
