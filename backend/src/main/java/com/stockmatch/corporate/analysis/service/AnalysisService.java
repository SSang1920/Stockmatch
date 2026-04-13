package com.stockmatch.corporate.analysis.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.corporate.analysis.dto.data.AnalysisPackage;
import com.stockmatch.corporate.analysis.dto.data.MissingDataItem;
import com.stockmatch.corporate.analysis.dto.data.UserContext;
import com.stockmatch.corporate.analysis.mapper.global.GlobalBusinessPerformanceMapper;
import com.stockmatch.corporate.analysis.mapper.global.GlobalFinancialHealthMapper;
import com.stockmatch.corporate.analysis.mapper.global.GlobalMarketMomentumMapper;
import com.stockmatch.corporate.analysis.mapper.korea.KoreaBusinessPerformanceMapper;
import com.stockmatch.corporate.analysis.mapper.korea.KoreaFinancialHealthMapper;
import com.stockmatch.corporate.global.balancesheet.service.BalancesheetService;
import com.stockmatch.corporate.global.cashflow.service.CachflowService;
import com.stockmatch.corporate.global.earnings.service.EarningsService;
import com.stockmatch.corporate.global.incomestatement.service.IncomeStatementService;
import com.stockmatch.corporate.global.news.service.NewsService;
import com.stockmatch.corporate.global.overview.service.OverviewService;
import com.stockmatch.corporate.korea.finance.service.KoreaFinanceService;
import com.stockmatch.portfolio.dto.HoldingResponse;
import com.stockmatch.portfolio.service.HoldingService;
import com.stockmatch.stock.domain.Market;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.repository.SecurityRepository;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import  com.stockmatch.corporate.korea.finance.dto.DartFinancialRawResponse.RawAccountItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final UserRepository userRepository;
    private final HoldingService holdingService;
    private final SecurityRepository securityRepository;

    private final GlobalBusinessPerformanceMapper businessMapper;
    private final GlobalMarketMomentumMapper momentumMapper;
    private final GlobalFinancialHealthMapper financialMapper;

    private final KoreaFinanceService koreaFinanceService;
    private final KoreaBusinessPerformanceMapper koreaBusinessMapper;
    private final KoreaFinancialHealthMapper koreaFinancialMapper;

    private final OverviewService overviewService;
    private final IncomeStatementService incomeService;
    private final BalancesheetService balancesheetService;
    private final CachflowService cashflowService;
    private final NewsService newsService;
    private final EarningsService earningsService;


    public AnalysisPackage analyzeStock(Long userId, String symbol){
        List<MissingDataItem> missingData = new ArrayList<>();

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Security security = securityRepository.findByTicker(symbol)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        UserContext userContext = buildAiUserContext(user,userId);

        AnalysisPackage.TargetStockInfo targetStockInfo = AnalysisPackage.TargetStockInfo.builder()
                .ticker(security.getTicker())
                .name(security.getName())
                .market(security.getMarket().name())
                .build();

        if (security.getMarket() == Market.KR) {
            return analyzeKoreanStock(userContext, symbol, missingData, targetStockInfo);
        }else{
            return analyzeGlobalStock(userContext, userId, symbol, missingData, user, targetStockInfo);
        }


    }

    private AnalysisPackage analyzeKoreanStock(UserContext userContext, String symbol, List<MissingDataItem> missingData, AnalysisPackage.TargetStockInfo targetStockInfo){
        String reportCode = "11011";

        List<RawAccountItem> currentReport = null;
        int currentYearInt = LocalDate.now().getYear();

        // 올해부터 역순으로 조회하며 레포트가 있는지 확인
        int lookBackDepth = 4;
        boolean found = false;

        for(int i =0; i < lookBackDepth; i++){
            int targetYear = currentYearInt - i;

            try {
                var result = koreaFinanceService.getFinancialData(symbol, String.valueOf(targetYear), reportCode);

                if (result != null && !result.getAccountItems().isEmpty() && result.getAccountItems() != null ) {
                    currentReport = result.getAccountItems();
                    currentYearInt =targetYear;
                    found = true;

                    log.info("{}년도에 해당하는 데이터를 찾았습니다.", targetYear);
                    break;
                }
            }  catch (Exception e){
                log.debug(" {} 년도 데이터가 없습니다. 이전 년도로 검색합니다.", targetYear);

            }
        }

        if (!found || currentReport == null) {
            missingData.add(new MissingDataItem("Business", "Year", "최근 4년 내의 보고서가 없습니다."));

            return AnalysisPackage.builder()
                    .user(userContext)
                    .targetStock(targetStockInfo)
                    .missingData(missingData)
                    .build();
        }

        // 검색된 보고서보다 1년전 보고서 호출
        String prevYear = String.valueOf(currentYearInt -1);

        var prevReport = safeFetch(() -> koreaFinanceService.getFinancialData(symbol, prevYear, reportCode), "Financial", "Report(Prev)", missingData);

        missingData.add(new MissingDataItem ("Market", "Momentum", "국내 주식 시장 뉴스는 지원되지 않습니다. "));


        return AnalysisPackage.builder()
                .user(userContext)
                .targetStock(targetStockInfo)
                .businessPerformance(koreaBusinessMapper.map(String.valueOf(currentYearInt), currentReport, prevReport != null ? prevReport.getAccountItems() : null))
                .financialHealth(koreaFinancialMapper.map(currentReport))
                .marketMomentum(null)
                .missingData(missingData)
                .build();
    }

    private AnalysisPackage analyzeGlobalStock(UserContext userContext, Long userId, String symbol, List<MissingDataItem> missingData, User user, AnalysisPackage.TargetStockInfo targetStockInfo){
        //데이터 수집
        var overview = safeFetch(()-> overviewService.getCompanyOverview(userId, symbol), "Market", "Overview", missingData);
        var income = safeFetch(()-> incomeService.getIncomeStatement(userId, symbol), "Business", "Income", missingData);
        var balance = safeFetch(()-> balancesheetService.getBalancesheet(userId, symbol), "Health", "BalanceSheet", missingData);
        var cash = safeFetch(()-> cashflowService.getCachflow(userId, symbol), "Health", "CashFlow", missingData);
        var news = safeFetch(()-> newsService.getNewsSentiment(symbol, user), "Market", "News", missingData);
        var earnings = safeFetch(()-> earningsService.getEarnings(userId, symbol), "Market", "Earnings", missingData);

        return AnalysisPackage.builder()
                .user(userContext)
                .targetStock(targetStockInfo)
                .businessPerformance(businessMapper.map(income, balance))
                .marketMomentum(momentumMapper.map(overview, news, earnings, symbol))
                .financialHealth(financialMapper.map(balance, cash))
                .missingData(missingData)
                .build();
    }

    public AnalysisPackage analyzeMyPortfolio(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        var profile = user.getInvestmentProfile();
        UserContext userContext = buildAiUserContext(user,userId);

        return AnalysisPackage.builder()
                .user(userContext)
                .build();
    }

    private UserContext buildAiUserContext(User user, Long userId) {
        //  원본 데이터 가져오기
        List<HoldingResponse> rawHoldings = holdingService.getMyHoldings(userId);

        //  총 자산 계산
        double totalValue = rawHoldings.stream()
                .mapToDouble(h -> h.quantity().multiply(h.avgPrice()).doubleValue())
                .sum();

        // 비중 계산 및 AI 전용 DTO로 변환
        List<UserContext.AiPortfolioHoldingDto> aiHoldings = rawHoldings.stream()
                .map(h -> {
                    double itemValue = h.quantity().multiply(h.avgPrice()).doubleValue();
                    double weight = totalValue > 0 ? Math.round((itemValue / totalValue) * 1000) / 10.0 : 0.0;

                    return UserContext.AiPortfolioHoldingDto.builder()
                            .ticker(h.ticker())
                            .name(h.name())
                            .quantity(h.quantity())
                            .avgPrice(h.avgPrice())
                            .currency(h.currency())
                            .amount(itemValue)
                            .weightPct(weight)
                            .build();
                })
                .toList();

        // 4. 완벽한 UserContext 반환
        var profile = user.getInvestmentProfile();
        return UserContext.builder()
                .investmentType(profile != null ? profile.getInvestmentType().name() : "UNDETERMINED")
                .investmentScore(profile != null ? profile.getTotalScore() : 0)
                .currentHoldings(aiHoldings) //
                .build();
    }

    private <T> T safeFetch(Supplier<T> fetcher, String section, String field, List<MissingDataItem> missingData) {
        try {
            T result = fetcher.get();
            if(result == null ) {
                throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
            }
                return result;

         } catch (Exception e) {
            log.error("Error fetching {} {}: {}", section, field, e.getMessage());

            String reason = "데이터가 누락되었습니다.";
            if (e.getMessage() != null && e.getMessage().contains("rate limit")) {
                reason ="일일 API 호출 한도를 초과했습니다. API를 변경하거나 내일 다시 시도해주세요.";
            } else if (e.getMessage() != null && e.getMessage().contains("Information")){
                reason = "데이터 제공처의 일시적인 제한이 있습니다. 잠시 후 시도해주세요.";
            }

            missingData.add(new MissingDataItem(section, field, reason));
            return null;
        }
    }


}
