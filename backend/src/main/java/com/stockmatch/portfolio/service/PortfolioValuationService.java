    package com.stockmatch.portfolio.service;

    import com.stockmatch.common.exception.BusinessException;
    import com.stockmatch.common.exception.ErrorCode;
    import com.stockmatch.exchangeRate.cache.ExchangeRateCacheService;
    import com.stockmatch.exchangeRate.domain.FromCurrency;
    import com.stockmatch.exchangeRate.domain.ToCurrency;
    import com.stockmatch.exchangeRate.service.FxRateService;
    import com.stockmatch.portfolio.domain.*;
    import com.stockmatch.portfolio.dto.HoldingValuationResponse;
    import com.stockmatch.portfolio.dto.PortfolioDailySummaryResponse;
    import com.stockmatch.portfolio.dto.PortfolioProfitStatsResponse;
    import com.stockmatch.portfolio.dto.PortfolioValuationResponse;
    import com.stockmatch.portfolio.repository.HoldingRepository;
    import com.stockmatch.portfolio.repository.PortfolioDailySummaryRepository;
    import com.stockmatch.portfolio.repository.PortfolioRepository;
    import com.stockmatch.portfolio.repository.TransactionRepository;
    import com.stockmatch.stock.domain.Security;
    import com.stockmatch.stock.dto.DailyPriceResponse;
    import com.stockmatch.stock.repository.SecurityRepository;
    import com.stockmatch.stock.service.DailyPriceService;
    import com.stockmatch.stock.service.StockPriceService;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.math.BigDecimal;
    import java.math.RoundingMode;
    import java.time.LocalDate;
    import java.time.temporal.TemporalAdjusters;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.stream.Collectors;

    @Slf4j
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
        private final ExchangeRateCacheService exchangeRateCacheService;
        private final PortfolioDailySummaryRepository dailySummaryRepository;

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

                        // 환율 조회 실패 시 예외 처리
                        if (usdToKrw == null) {
                            throw new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND);
                        }
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
        public PortfolioValuationResponse calculate(long portfolioId) {
            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

            // 유저 생성일 추출
            String userCreatedAt = portfolio.getUser().getCreatedAt().toString();

            // 보유 종목 조회
            var holdings = holdingRepository.findAllWithSecurityByPortfolioId(portfolioId);
            if (holdings.isEmpty()) {
                return new PortfolioValuationResponse(
                        portfolioId,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        getUsdToKrwRate(LocalDate.now()),
                        new ArrayList<>(),
                        userCreatedAt,
                        BigDecimal.ZERO
                );
            }

            // 모든 Security 객체 추출
            List<Security> securities = holdings.stream()
                    .map(Holding::getSecurity)
                    .toList();

            // 한 번에 시세 조회
            Map<String, BigDecimal> priceMap = stockPriceService.getBulkPrices(securities);

            LocalDate today = LocalDate.now();
            BigDecimal usdToKrw = getUsdToKrwRate(today);

            BigDecimal totalInvested = BigDecimal.ZERO;     // 총 매입금액
            BigDecimal totalValue = BigDecimal.ZERO;        // 총 평가금액

            List<HoldingValuationResponse> details = new ArrayList<>();

            for (Holding h : holdings) {
                // 종목 기본 정보
                var s = h.getSecurity();
                var ticker = s.getTicker();

                String name = s.getName();
                String krName = name;

                String currency;
                if (s.getCurrency() != null) {
                    currency = s.getCurrency().name();
                } else {
                    currency = s.isKorean() ? "KRW" : "USD";
                }

                BigDecimal current = priceMap.getOrDefault(ticker, BigDecimal.ZERO)
                        .setScale(SCALE_PRICE, RoundingMode.HALF_UP);

                // 보유 수량/평단가
                var qty = nz(h.getQuantity());
                var avg = nz(h.getAvgPrice());

                // 환율 결정
                BigDecimal fx = BigDecimal.ONE;

                if (!s.isKorean()) {
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
                        h.getId(),
                        ticker,
                        name,
                        krName,
                        currency,
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
            BigDecimal unrealizedPnl = totalValue.subtract(totalInvested);
            BigDecimal realizedPnl = portfolio.getRealizedPnl();

            BigDecimal totalPnl = unrealizedPnl.add(realizedPnl).setScale(SCALE_MONEY, RoundingMode.HALF_UP);

            BigDecimal totalRate = totalInvested.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : totalPnl.divide(totalInvested, SCALE_RATE, RoundingMode.HALF_UP);

            // 결과 반환
            return new PortfolioValuationResponse(
                    portfolioId,
                    totalInvested,
                    totalValue,
                    totalPnl,
                    totalRate,
                    usdToKrw,
                    details,
                    userCreatedAt,
                    realizedPnl
            );
        }

        @Transactional(readOnly = true)
        public List<PortfolioDailySummaryResponse> calculateDailyHistory(Long portfolioId, LocalDate from, LocalDate to) {
            // DB에서 날짜 범위에 맞는 기록 조회
            List<PortfolioDailySummary> summaries = dailySummaryRepository
                    .findByPortfolioIdAndDateBetweenOrderByDateAsc(portfolioId, from, to);

            // 엔티티를 DTO 변환
            return summaries.stream()
                    .map(summary -> new PortfolioDailySummaryResponse(
                            summary.getDate(),
                            summary.getTotalInvested(),
                            summary.getTotalValue(),
                            summary.getTotalPnl(),
                            summary.getTotalRate()
                    ))
                    .toList();
        }

        public PortfolioProfitStatsResponse getAdvancedStats(Long portfolioId, String year, String month) {
            // 실시간 현재 상태 조회
            PortfolioValuationResponse current = calculate(portfolioId);
            BigDecimal nowRealized = current.realizedPnl();

            // 날짜 및 현재 여부 파악
            LocalDate now = LocalDate.now();
            int y = Integer.parseInt(year);
            int m = month.equals("annual") ? (y == now.getYear() ? now.getMonthValue() : 12) : Integer.parseInt(month);

            boolean isCurrentYear = (y == now.getYear());
            boolean isCurrentMonth = isCurrentYear && (m == now.getMonthValue());

            // 비교 기준점 날짜 설정
            LocalDate selectedMonthEnd = LocalDate.of(y, m, 1).with(TemporalAdjusters.lastDayOfMonth());
            LocalDate prevMonthEnd = selectedMonthEnd.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
            LocalDate lastYearEnd = LocalDate.of(y - 1, 12, 31);
            LocalDate yesterday = now.minusDays(1);

            // 각 기준점의 누적 실현 수익 조회
            BigDecimal realizedAtSelectedMonth = getSnapshotRealizedPnl(portfolioId, selectedMonthEnd);
            BigDecimal realizedAtLastYear = getSnapshotRealizedPnl(portfolioId, lastYearEnd);
            BigDecimal realizedAtPrevMonth = getSnapshotRealizedPnl(portfolioId, prevMonthEnd);
            BigDecimal realizedAtYesterday = getSnapshotRealizedPnl(portfolioId, yesterday);

            // 총 수익
            BigDecimal totalProfit = nowRealized;

            // 연간 수익
            BigDecimal annualProfit = nowRealized.subtract(realizedAtLastYear);

            // 월간 수익
            BigDecimal monthlyProfit = nowRealized.subtract(realizedAtPrevMonth);

            // 일간 수익
            BigDecimal dailyProfit = nowRealized.subtract(realizedAtYesterday);

            return new PortfolioProfitStatsResponse(
                    current.totalPnlAmount(),
                    current.totalPnlRate().doubleValue(),
                    annualProfit,
                    calculateRate(annualProfit, current.totalInvested()),
                    monthlyProfit,
                    calculateRate(monthlyProfit, current.totalInvested()),
                    dailyProfit,
                    calculateRate(dailyProfit, current.totalInvested()),
                    nowRealized
            );
        }

        /**
         * 스냅샷에서 실현 손익 값을 가져오는 헬퍼 메소드
         */
        private BigDecimal getSnapshotRealizedPnl(Long portfolioId, LocalDate date) {
            return dailySummaryRepository.findFirstByPortfolioIdAndDateLessThanEqualOrderByDateDesc(portfolioId, date)
                    .map(PortfolioDailySummary::getRealizedPnl)
                    .orElse(BigDecimal.ZERO);
        }

        private Double calculateRate(BigDecimal current, BigDecimal base) {
            if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
                return 0.0;
            }
            return current.divide(base, 6, RoundingMode.HALF_UP)
                    .subtract(BigDecimal.ONE)
                    .doubleValue() * 100;
        }

        /**
         * 최신 USD -> KRW 환율 조회 (Redis 캐시 우선 확인)
         */
        private BigDecimal getUsdToKrwRate(LocalDate date) {
            try {
                // 캐시 확인
                BigDecimal cachedRate = exchangeRateCacheService.getCurrentRate(FromCurrency.USD, ToCurrency.KRW);
                if (cachedRate != null) return cachedRate;

                // DB 또는 외부 API 조회
                BigDecimal latestRate = fxRateService.getLatestUsdToKrwRate(date);
                if (latestRate == null) {
                    throw new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND);
                }

                // 캐시 저장
                exchangeRateCacheService.saveCurrentRate(FromCurrency.USD, ToCurrency.KRW, latestRate);
                return latestRate;

            } catch (Exception e) {
                log.error("환율 조회 중 오류 발생", e);
                throw new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND);
            }
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
