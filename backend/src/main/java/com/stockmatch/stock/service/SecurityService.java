package com.stockmatch.stock.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.dto.SecurityRequest;
import com.stockmatch.stock.dto.SecurityResponse;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private final SecurityRepository securityRepository;

    /**
     * 신규 종목 등록
     */
    public SecurityResponse createSecurity(SecurityRequest request) {

        // ticker 중복 방지
        securityRepository.findByTicker(request.getTicker()).ifPresent(s -> {
            throw new BusinessException(ErrorCode.DUPLICATE_TICKER);
        });

        // 새 Security 엔티티 생성
        Security security = new Security(
                null,
                request.getTicker(),
                request.getName(),
                request.getMarket(),
                request.getExchange(),
                request.getCurrency(),
                request.getType(),
                false,
                null,
                null
        );

        securityRepository.save(security);

        // DTO 변환
        return SecurityResponse.builder()
                .id(security.getId())
                .ticker(security.getTicker())
                .name(security.getName())
                .market(security.getMarket())
                .exchange(security.getExchange())
                .currency(security.getCurrency())
                .type(security.getType())
                .delisted(security.isDelisted())
                .build();
    }
}
