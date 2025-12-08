    package com.stockmatch.portfolio.service;

    import com.stockmatch.common.exception.BusinessException;
    import com.stockmatch.common.exception.ErrorCode;
    import com.stockmatch.exchangeRate.service.FxRateService;
    import com.stockmatch.portfolio.domain.Holding;
    import com.stockmatch.portfolio.dto.HoldingValuationResponse;
    import com.stockmatch.portfolio.dto.PortfolioDailySummaryResponse;
    import com.stockmatch.portfolio.dto.PortfolioValuationResponse;
    import com.stockmatch.portfolio.repository.HoldingRepository;
    import com.stockmatch.stock.domain.Security;
    import com.stockmatch.stock.dto.DailyPriceResponse;
    import com.stockmatch.stock.service.DailyPriceService;
    import com.stockmatch.stock.service.StockPriceService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.math.BigDecimal;
    import java.math.RoundingMode;
    import java.time.LocalDate;
    import java.util.ArrayList;
    import java.util.List;

    @Service
    @RequiredArgsConstructor
    @Transactional(readOnly = true)
    public class PortfolioValuationService {

        private static final int SCALE_PRICE = 4;
        private static final int SCALE_MONEY = 2;
        private static final int SCALE_RATE  = 6;
        private static final int MAX_LOOKBACK_DAYS_FOR_PRICE = 7;

        private final HoldingRepository holdingRepository;
        private final StockPriceService stockPriceService;
        private final FxRateService fxRateService;
        private final DailyPriceService dailyPriceService;

        /**
         * 포트폴리오의 보유 종목을 기준으로 from ~ to 구간의 일자별 평가 지표 계산
         */
        @Transactional(readOnly = false)
        public List<PortfolioDailySummaryResponse> calculateDailyHistory(long portfolioId, LocalDate from, LocalDate to) {
            // 파라미터 검증
            if (from == null || to == null || from.isAfter(to)) {
                throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
            }

            // 포트폴리오 보유 종목 조회
            List<Holding> holdings = holdingRepository.findAllWithSecurityByPortfolioId(portfolioId);
            if (holdings.isEmpty()) {
                throw new BusinessException(ErrorCode.HOLDING_NOT_FOUND);
            }

            // 결과 리스트
            List<PortfolioDailySummaryResponse> result = new ArrayList<>();

            // 날짜 루프
            LocalDate currentDate = from;
            while (!currentDate.isAfter(to)) {
                // 일자별 평가 계산
                PortfolioDailySummaryResponse daily = calculateOneDay(portfolioId, holdings, currentDate);

                result.add(daily);

                currentDate = currentDate.plusDays(1);
            }

            return result;
        }

        /**
         * 특정 일자 기준으로 포트폴리오 전체 평가를 계산
         */
        private PortfolioDailySummaryResponse calculateOneDay(long portfolioId, List<Holding> holdings, LocalDate date) {
            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalValue = BigDecimal.ZERO;

            BigDecimal usdToKrw = null;

            for (Holding h : holdings) {
                Security security = h.getSecurity();
                String ticker = security.getTicker();

                // 보유 수량/평단가
                BigDecimal quantity = nz(h.getQuantity());
                BigDecimal avg = nz(h.getAvgPrice());

                // 수량 0이면 스킵
                if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                // 해당 일자의 종가 조회
                BigDecimal closingPrice = getClosingPriceOnDate(security, ticker, date)
                        .setScale(SCALE_PRICE, RoundingMode.HALF_UP);

                // 환율 결정
                BigDecimal fx = BigDecimal.ONE;

                if (!security.isKorean()) {
                    // 미국주식인 경우 USD -> KRW 환산
                    if (usdToKrw == null) {
                        // 해당 일자 기준 최신 환율 사용
                        usdToKrw = fxRateService.getLatestUsdToKrwRate(date);
                    }

                    fx = usdToKrw;
                }

                // 평단/현재가를 KRW 기준으로 변환
                BigDecimal avgKrw = avg.multiply(fx).setScale(SCALE_PRICE, RoundingMode.HALF_UP);
                BigDecimal closingKrw = closingPrice.multiply(fx).setScale(SCALE_PRICE, RoundingMode.HALF_UP);

                // 매입금액/평가금액 (KRW 기준)
                BigDecimal invested = avgKrw.multiply(quantity).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
                BigDecimal value = closingKrw.multiply(quantity).setScale(SCALE_MONEY, RoundingMode.HALF_UP);

                // 포트폴리오 합계 누적
                totalInvested = totalInvested.add(invested);
                totalValue = totalValue.add(value);
            }

            // 총 손익/총 수익률 계산
            BigDecimal totalPnl = totalValue.subtract(totalInvested).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
            BigDecimal totalRate = totalInvested.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : totalValue.divide(totalInvested, SCALE_RATE, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);

            return new PortfolioDailySummaryResponse(
                    date,
                    totalInvested,
                    totalValue,
                    totalPnl,
                    totalRate
            );
        }

        /**
         * 포트폴리오의 보유 종목을 조회하여 실시간 시세 기반으로 평가 지표 계산
         */
        @Transactional(readOnly = false)
        public PortfolioValuationResponse calculate(long portfolioId) {
            // 보유 종목 조회
            var holdings = holdingRepository.findAllWithSecurityByPortfolioId(portfolioId);
            if (holdings.isEmpty()) {
                throw new BusinessException(ErrorCode.HOLDING_NOT_FOUND);
            }

            LocalDate today = LocalDate.now();
            BigDecimal usdToKrw = null;

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

                // 환율 결정
                BigDecimal fx = BigDecimal.ONE;

                if (!s.isKorean()) {
                    if (usdToKrw == null) {
                        usdToKrw = fxRateService.getLatestUsdToKrwRate(today);
                    }
                    fx = usdToKrw;
                }

                // 평단가/현재가를 KRW 기준으로 변환
                BigDecimal avgKrw = avg.multiply(fx).setScale(SCALE_PRICE, RoundingMode.HALF_UP);
                BigDecimal CurrentKrw = current.multiply(fx).setScale(SCALE_PRICE, RoundingMode.HALF_UP);

                // 매입금액/평가금액/손익 (KRW 기준)
                BigDecimal invested = avgKrw.multiply(qty).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
                BigDecimal value = CurrentKrw.multiply(qty).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
                BigDecimal pnl = value.subtract(invested).setScale(SCALE_MONEY, RoundingMode.HALF_UP);

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
                    usdToKrw,
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
         * 특정 일자의 종가 조회
         */
        private BigDecimal getClosingPriceOnDate(Security security, String ticker, LocalDate date) {
            // 미래 날짜 호출시 예외처리
            LocalDate today = LocalDate.now();
            if (date.isAfter(today)) {
                throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
            }

            LocalDate cursor = date;

            for (int i = 0; i < MAX_LOOKBACK_DAYS_FOR_PRICE; i++) {
                List<DailyPriceResponse> prices = dailyPriceService.getDailyPrices(ticker, cursor, cursor);

                if (!prices.isEmpty()) {
                    DailyPriceResponse price = prices.get(0);

                    return price.closePrice();
                }

                // 이 날짜에 시세 없으면 하루 전으로 이동
                cursor = cursor.minusDays(1);
            }

            // date ~ date-7일 동안 시세가 없을 경우
            throw new BusinessException(ErrorCode.DAILY_PRICE_NOT_FOUND);
        }

        /**
         * null -> 0 변환
         */
        private BigDecimal nz(BigDecimal v) {
            return v == null ? BigDecimal.ZERO : v;
        }
    }
