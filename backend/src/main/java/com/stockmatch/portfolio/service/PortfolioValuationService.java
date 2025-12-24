    package com.stockmatch.portfolio.service;

    import com.stockmatch.common.exception.BusinessException;
    import com.stockmatch.common.exception.ErrorCode;
    import com.stockmatch.exchangeRate.service.FxRateService;
    import com.stockmatch.portfolio.domain.Holding;
    import com.stockmatch.portfolio.domain.TradeType;
    import com.stockmatch.portfolio.domain.Transaction;
    import com.stockmatch.portfolio.dto.HoldingValuationResponse;
    import com.stockmatch.portfolio.dto.PortfolioDailySummaryResponse;
    import com.stockmatch.portfolio.dto.PortfolioValuationResponse;
    import com.stockmatch.portfolio.repository.HoldingRepository;
    import com.stockmatch.portfolio.repository.PortfolioRepository;
    import com.stockmatch.portfolio.repository.TransactionRepository;
    import com.stockmatch.stock.domain.Security;
    import com.stockmatch.stock.dto.DailyPriceResponse;
    import com.stockmatch.stock.repository.SecurityRepository;
    import com.stockmatch.stock.service.DailyPriceService;
    import com.stockmatch.stock.service.StockPriceService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.math.BigDecimal;
    import java.math.RoundingMode;
    import java.time.LocalDate;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    @Transactional(readOnly = true)
    public class PortfolioValuationService {

        private static final int SCALE_PRICE = 4;
        private static final int SCALE_MONEY = 2;
        private static final int SCALE_RATE  = 6;
        private static final int MAX_LOOKBACK_DAYS_FOR_PRICE = 7;

        private final HoldingRepository holdingRepository;
        private final TransactionRepository transactionRepository;
        private final SecurityRepository securityRepository;
        private final PortfolioRepository portfolioRepository;
        private final StockPriceService stockPriceService;
        private final FxRateService fxRateService;
        private final DailyPriceService dailyPriceService;

        /**
         * 거래내역을 리플레이하여 from ~ to 구간의 일자별 평가 요약을 계산
         * 매수 이전/전량 매도 후 기간은 자동으로 수량 0 -> 평가금 0
         * 휴장일/주말: 전 거래일 종가 사용
         */
        public List<PortfolioDailySummaryResponse> calculateDailyHistory(long portfolioId, LocalDate from, LocalDate to) {
            // 파라미터 검증
            if (from == null || to == null || from.isAfter(to)) {
                throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
            }

            // 포트폴리오 존재 여부 검증
            boolean exists = portfolioRepository.existsById(portfolioId);
            if (!exists) {
                throw new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND);
            }

            // 포트폴리오 거래내역 모두 조회
            List<Transaction> transactions = transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolioId);

            if (transactions.isEmpty()) {
                throw new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND);
            }

            // 실제 평가 시작일
            LocalDate firstTradeDate = transactions.get(0).getTradeAt().toLocalDate();
            LocalDate startDate = from.isBefore(firstTradeDate) ? firstTradeDate : from;

            if (startDate.isAfter(to)) {
                // 요청 구간 전체가 첫 거래일 이전인 경우 -> 결과 X
                return List.of();
            }

            // 거래 리플레이 + 일자별 평가
            return replayAndCalculateDailyHistory(transactions, startDate, to);
        }

        /**
         * 거래내역을 날짜 순으로 리플레이하면서 startDate ~ endDate 구간의 일별 평가 계산
         */
        private List<PortfolioDailySummaryResponse> replayAndCalculateDailyHistory(List<Transaction> transactions, LocalDate startDate, LocalDate endDate) {
            List<PortfolioDailySummaryResponse> result = new ArrayList<>();

            // 종목별 포지션 상태 (securityId -> PositionState)
            HashMap<Long, PositionState> positionMap = new HashMap<>();

            // 거래에 등장하는 종목 securityId 목록 수집
            List<Long> securityIds = transactions.stream()
                    .map(transaction -> transaction.getSecurity().getId())
                    .distinct()
                    .toList();

            Map<Long, Security> securityMap = securityRepository.findAllById(securityIds).stream()
                    .collect(Collectors.toMap(
                            Security::getId,
                            s -> s
                    ));

            // 거래가 있는데 종목이 DB에 없으면 데이터 이상
            if (securityMap.size() != securityIds.size()) {
                throw new BusinessException(ErrorCode.SECURITY_NOT_FOUND);
            }

            int index = 0;
            int transactionCount = transactions.size();

            // startDate 이전 거래를 먼저 모두 반영해서 시작 시점 포지션 맞춤
            while (index < transactionCount && transactions.get(index).getTradeAt().toLocalDate().isBefore(startDate)) {
                applyTransaction(positionMap, transactions.get(index++));
            }

            // 날짜 루프
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                // current 날짜의 거래 반영
                while (index < transactionCount && transactions.get(index).getTradeAt().toLocalDate().isEqual(current)) {
                    applyTransaction(positionMap, transactions.get(index++));
                }

                // 해당 날짜 기준 포트폴리오 평가
                PortfolioDailySummaryResponse daily = evaluatePortfolioOnDate(positionMap, securityMap, current);

                result.add(daily);

                current = current.plusDays(1);
            }

            return result;
        }

        /**
         * 단일 거래를 종목별 포지션 상태에 반영
         * BUY / INITIAL_BUY : 수량 +, 원가 += price * quantity
         * SELL              : 수량 -, 원가 -= 평단 * quantity
         */
        private void applyTransaction(HashMap<Long, PositionState> positionMap, Transaction transaction) {
            Long securityId = transaction.getSecurity().getId();
            PositionState state = positionMap.computeIfAbsent(securityId, id -> new PositionState());

            BigDecimal quantity = nz(transaction.getQuantity());
            BigDecimal price = nz(transaction.getPrice());

            if (transaction.getType() == TradeType.BUY || transaction.getType() == TradeType.INITIAL_BUY) {
                // 매수: 수량/원가 증가
                BigDecimal additionalCost = price.multiply(quantity).setScale(SCALE_MONEY, RoundingMode.HALF_UP);

                state.quantity = state.quantity.add(quantity);
                state.cost = state.cost.add(additionalCost);
            } else if (transaction.getType() == TradeType.SELL) {
                // 예외 처리
                if (state.quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    return;
                }

                BigDecimal currentAvg = state.getAvgPrice();

                // 실제 매도 수량이 보유 수량보다 크면 보유 수량까지만 처리
                BigDecimal sellQuantity = quantity;
                if (sellQuantity.compareTo(state.quantity) > 0) {
                    sellQuantity = state.quantity;
                }

                BigDecimal costToRemove = currentAvg.multiply(sellQuantity).setScale(SCALE_MONEY, RoundingMode.HALF_UP);

                state.quantity = state.quantity.subtract(sellQuantity);
                state.cost = state.cost.subtract(costToRemove);

                if (state.quantity.compareTo(BigDecimal.ZERO) == 0) {
                    // 수량 0 이면 cost도 0으로 처리
                    state.cost = BigDecimal.ZERO;
                }
            }
        }

        /**
         * 특정 일자 기준으로 포트폴리오 전체 평가를 계산
         */
        private PortfolioDailySummaryResponse evaluatePortfolioOnDate(HashMap<Long, PositionState> positionMap, Map<Long, Security> securityMap, LocalDate date) {
            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalValue = BigDecimal.ZERO;

            BigDecimal usdToKrw = null;

            for (var entry : positionMap.entrySet()) {
                Long securityId = entry.getKey();
                PositionState state = entry.getValue();

                // 수량 0이면 스킵
                if (state.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                // 종목 조회
                Security security = securityMap.get(securityId);
                if (security == null) {
                    throw new BusinessException(ErrorCode.SECURITY_NOT_FOUND);
                }

                BigDecimal quantity = state.getQuantity();
                BigDecimal avg = state.getAvgPrice();

                // 해당 일자의 종가 조회
                BigDecimal closingPrice = getClosingPriceOnDate(security, security.getTicker(), date)
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
            return price;
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

        /**
         * 거래 리플레이 시 사용되는 종목별 포지션 상태
         */
        private static final class PositionState {
            private BigDecimal quantity;
            private BigDecimal cost;

            private PositionState() {
                this.quantity = BigDecimal.ZERO;
                this.cost = BigDecimal.ZERO;
            }

            private BigDecimal getQuantity() {
                return quantity;
            }

            private BigDecimal getAvgPrice() {
                if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                    return BigDecimal.ZERO;
                }
                return cost.divide(quantity, SCALE_PRICE, RoundingMode.HALF_UP);
            }
        }
    }
