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

        public PortfolioValuationResponse calculate(long portfolioId) {
            var holdings = holdingRepository.findAllWithSecurityByPortfolioId(portfolioId);
            if (holdings.isEmpty()) {
                throw new BusinessException(ErrorCode.HOLDING_NOT_FOUND);
            }

            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalValue = BigDecimal.ZERO;
            List<HoldingValuationResponse> details = new ArrayList<>();

            for (Holding h : holdings) {
                var s = h.getSecurity();
                var ticker = s.getTicker();
                var name = s.getName();

                var qty = nz(h.getQuantity());
                var avg = nz(h.getAvgPrice());

                var current = getCurrentPrice(s, ticker).setScale(SCALE_PRICE, RoundingMode.HALF_UP);


            }
        }

        private BigDecimal getCurrentPrice(Security security, String ticker) {
            var price = security.isKorean()
                    ? stockPriceService.getKrStockPrice(ticker).getCurrentPrice()
                    : stockPriceService.getUsStockPrice(ticker).getCurrentPrice();
            return BigDecimal.valueOf(price);
        }

        private BigDecimal nz(BigDecimal v) {
            return v == null ? BigDecimal.ZERO : v;
        }
    }
