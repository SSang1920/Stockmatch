package com.stockmatch.stock.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
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
    private final SecurityRepository securityRepository;

    /**
     * 거래량 순위 조회
     */
    public List<StockTrendResponse> getMostActive(int limit) {
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

        // 거래량 기준으로 내림차순 정렬 후 상위 N개 추출
        List<KisVolumeClient.KisOverseasVolumeItem> topItems = allItems.stream()
                .filter(item -> item.getTvol() != null && !item.getTvol().isEmpty())
                .sorted(Comparator.comparingLong((KisVolumeClient.KisOverseasVolumeItem item) ->
                        Long.parseLong(item.getTvol())).reversed())
                .limit(limit)
                .toList();

        // DB 데이터와 병합하여 DTO 반환
        return mergeWithDbData(topItems);
    }

    /**
     * 급등 순위 조회
     */
    public List<StockTrendResponse> getGainers() {
        return Collections.emptyList();
    }

    /**
     * 급락 순위 조회
     */
    public List<StockTrendResponse> getLosers() {
        return Collections.emptyList();
    }

    /**
     * API 데이터 + DB 데이터 병합 로직
     */
    private List<StockTrendResponse> mergeWithDbData(List<KisVolumeClient.KisOverseasVolumeItem> apiItems) {
        // 티커 목록 추출
        List<String> tickers = apiItems.stream()
                .map(item -> parseTicker(item.getRsym()))
                .toList();

        // DB에서 티커로 조회
        List<Security> securities = securityRepository.findByTickerIn(tickers);
        Map<String, Security> securityMap = securities.stream()
                .collect(Collectors.toMap(Security::getTicker, Function.identity()));

        // 매핑
        return apiItems.stream()
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

                    return mapToDto(item, finalName, cleanTicker);
                })
                .collect(Collectors.toList());
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
     * DTO 변환 헬퍼
     */
    private StockTrendResponse mapToDto(KisVolumeClient.KisOverseasVolumeItem item, String name, String ticker) {
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
}
