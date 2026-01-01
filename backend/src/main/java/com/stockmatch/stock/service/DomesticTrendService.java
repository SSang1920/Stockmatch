package com.stockmatch.stock.service;

import com.stockmatch.stock.client.kis.KisRankClient;
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

    private final KisRankClient kisRankClient;
    private final SecurityRepository securityRepository;

    /**
     * 거래량 순위 조회
     */
    public List<StockTrendResponse> getMostActive(int limit) {
        // API 호출
        List<KisRankClient.KisVolumeItem> rawData = kisRankClient.getDomesticVolumeRank();

        // DB 병합 및 DTO 변환
        return mergeWithDbData(rawData, limit);
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
    private List<StockTrendResponse> mergeWithDbData(List<KisRankClient.KisVolumeItem> apiItems, int limit) {
        if (apiItems == null || apiItems.isEmpty()) {
            return Collections.emptyList();
        }

        // 상위 N개 티커 추출
        List<KisRankClient.KisVolumeItem> targetItems = apiItems.stream()
                .limit(limit)
                .toList();

        List<String> tickers = targetItems.stream()
                .map(KisRankClient.KisVolumeItem::getMkscShrnIscd)
                .toList();

        // DB Bulk 조회
        List<Security> securities = securityRepository.findByTickerIn(tickers);
        Map<String, Security> securityMap = securities.stream()
                .collect(Collectors.toMap(Security::getTicker, Function.identity()));

        // 변환 및 합치기
        return targetItems.stream()
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
    private StockTrendResponse mapToDto(KisRankClient.KisVolumeItem item, String name) {
        try {
            long currentPrice = Long.parseLong(item.getStckPrpr().replace(",", ""));
            long changeValue = Long.parseLong(item.getPrdyVrss().replace(",", ""));
            double changeRate = Double.parseDouble(item.getPrdyCtrt().replace(",", ""));

            String priceStr = String.format("%,d원", currentPrice);
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
            return null;
        }
    }
}
