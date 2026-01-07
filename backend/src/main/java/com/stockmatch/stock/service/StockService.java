package com.stockmatch.stock.service;

import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.dto.StockSearchResponse;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final SecurityRepository securityRepository;

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
