    package com.stockmatch.portfolio.service;

    import com.stockmatch.common.exception.BusinessException;
    import com.stockmatch.common.exception.ErrorCode;
    import com.stockmatch.portfolio.domain.Holding;
    import com.stockmatch.portfolio.dto.HoldingValuationResponse;
    import com.stockmatch.portfolio.dto.PortfolioValuationResponse;
    import com.stockmatch.portfolio.repository.HoldingRepository;
    import com.stockmatch.stock.domain.Security;
    import com.stockmatch.stock.service.StockPriceService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.math.BigDecimal;
    import java.math.RoundingMode;
    import java.util.ArrayList;
    import java.util.List;

    @Service
    @RequiredArgsConstructor
    @Transactional(readOnly = true)
    public class PortfolioValuationService {

        private static final int SCALE_PRICE = 4;
        private static final int SCALE_MONEY = 2;
        private static final int SCALE_RATE  = 6;

        private final HoldingRepository holdingRepository;
        private final StockPriceService stockPriceService;

        /**
         * 포트폴리오의 보유 종목을 조회하여 실시간 시세 기반으로 평가 지표 계산
         */
        public PortfolioValuationResponse calculate(long portfolioId) {
            // 보유 종목 조회
            var holdings = holdingRepository.findAllWithSecurityByPortfolioId(portfolioId);
            if (holdings.isEmpty()) {
                throw new BusinessException(ErrorCode.HOLDING_NOT_FOUND);
            }

            BigDecimal totalInvested = BigDecimal.ZERO;     // 총 매입금액
            BigDecimal totalValue = BigDecimal.ZERO;        // 총 평가금액
            List<HoldingValuationResponse> details = new ArrayList<>();

            for (Holding h : holdings) {
                // 종목 기본 정보
                var s = h.getSecurity();
                var ticker = s.getTicker();
                var name = s.getName();

                // 보유 수량/평단가
                var qty = nz(h.getQuantity());
                var avg = nz(h.getAvgPrice());

                // 현재가 조회
                BigDecimal current = getCurrentPrice(s, ticker).setScale(SCALE_PRICE, RoundingMode.HALF_UP);

                // 매입금액 = 평단가 * 수량
                var invested = avg.multiply(qty).setScale(SCALE_MONEY, RoundingMode.HALF_UP);

                // 평가금액 = 현재가 * 수량
                var value = current.multiply(qty).setScale(SCALE_MONEY, RoundingMode.HALF_UP);

                // 평가손익 = 평가금액 - 매입금액
                var pnl = value.subtract(invested).setScale(SCALE_MONEY, RoundingMode.HALF_UP);

                // 수익률 = (평가금액 / 매입금액) - 1
                BigDecimal pnlRate = invested.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : value.divide(invested, SCALE_RATE, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);

                // 포트폴리오 합계 누적
                totalInvested = totalInvested.add(invested);
                totalValue = totalValue.add(value);

                // 종목별 상세 결과 적재
                details.add(new HoldingValuationResponse(
                        ticker,
                        name,
                        qty,
                        avg,
                        current,
                        invested,
                        value,
                        pnl,
                        pnlRate
                ));
            }

            // 총 손익 및 총 수익률
            var totalPnl = totalValue.subtract(totalInvested).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
            var totalRate = totalInvested.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : totalValue.divide(totalInvested, SCALE_RATE, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);

            // 결과 반환
            return new PortfolioValuationResponse(
                    portfolioId,
                    totalInvested,
                    totalValue,
                    totalPnl,
                    totalRate,
                    details
            );
        }

        /**
         * 시장 구분에 따라 현재가 조회
         */
        private BigDecimal getCurrentPrice(Security security, String ticker) {
            var price = security.isKorean()
                    ? stockPriceService.getKrStockPrice(ticker).getCurrentPrice()
                    : stockPriceService.getUsStockPrice(ticker).getCurrentPrice();
            return BigDecimal.valueOf(price);
        }

        /**
         * null -> 0 변환
         */
        private BigDecimal nz(BigDecimal v) {
            return v == null ? BigDecimal.ZERO : v;
        }
    }
