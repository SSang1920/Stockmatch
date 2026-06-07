package com.stockmatch.stock.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.client.PriceClientRouter;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.dto.StockSearchResponse;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final SecurityRepository securityRepository;
    private final PriceClientRouter priceClientRouter;

    public List<StockSearchResponse> searchStocks(String query) {
        // 검색어가 없으면 빈 결과 반환
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        // 상위 10개 조회
        List<Security> results = securityRepository.searchByKeyword(
                query.trim(),
                PageRequest.of(0, 10)
        );

        // Entity -> DTO 변환
        return results.stream()
                .map(this::toSearchResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StockSearchResponse getOrCreateNewStock(String ticker, String market) {
        String trimmedTicker = ticker.trim().toUpperCase();
        String trimmedMarket = market.trim().toUpperCase();

        // 내 로컬 데이터베이스 캐시 테이블에 존재하는지 조회
        Optional<Security> existingStock = securityRepository.findByTicker(trimmedTicker);
        if (existingStock.isPresent()) {
            return toSearchResponse(existingStock.get());
        }

        log.info("[신규 티커 발견]: 데이터베이스에 [{}] 종목이 없습니다.", trimmedTicker);

        try {
            // 주입된 priceClientRouter가 내부 KIS 클라이언트를 선택하여 기업 프로필 수집 수행
            Security newSecurity = priceClientRouter.fetchCompanyProfile(trimmedTicker, trimmedMarket);

            if (newSecurity == null) {
                log.warn("KIS 외부 금융망에서도 [{}] 종목 정보를 찾을 수 없습니다.", trimmedTicker);
                throw new BusinessException(ErrorCode.SECURITY_NOT_FOUND);
            }

            // JpaAuditing "SYSTEM" 안전 궤도를 타고 비로그인 유저 상태에서도 영속성 컨텍스트 즉시 적재
            Security savedSecurity = securityRepository.saveAndFlush(newSecurity);

            log.info("새로운 KIS 연동 티커 [{}] 내 자산 테이블 자동 영구 빌드", trimmedTicker);
            return toSearchResponse(savedSecurity);

        } catch (Exception e) {
            log.error("새로운 티커 [{}] KIS 수집 파이프라인 구동 중 예외 발생 원인: ", trimmedTicker, e);
            throw new BusinessException(ErrorCode.SECURITY_NOT_FOUND);
        }
    }

    private StockSearchResponse toSearchResponse(Security security) {
        return StockSearchResponse.builder()
                .id(security.getId())
                .ticker(security.getTicker())
                .name(security.getName())
                .englishName(security.getEnglishName())
                .market(security.getMarket())
                .exchange(security.getExchange() != null ? security.getExchange().name() : "")
                .build();
    }
}
