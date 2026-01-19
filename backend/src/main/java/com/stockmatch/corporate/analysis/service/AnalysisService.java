package com.stockmatch.corporate.analysis.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.corporate.analysis.dto.AnalysisPackage;
import com.stockmatch.corporate.analysis.dto.MissingDataItem;
import com.stockmatch.corporate.analysis.dto.UserContext;
import com.stockmatch.corporate.analysis.mapper.BusinessPerformanceMapper;
import com.stockmatch.corporate.analysis.mapper.FinancialHealthMapper;
import com.stockmatch.corporate.analysis.mapper.MarketMomentumMapper;
import com.stockmatch.corporate.global.balancesheet.dto.BalancesheetDto;
import com.stockmatch.corporate.global.balancesheet.service.BalancesheetService;
import com.stockmatch.corporate.global.cashflow.dto.CashflowDto;
import com.stockmatch.corporate.global.cashflow.service.CachflowService;
import com.stockmatch.corporate.global.earnings.dto.EarningsDto;
import com.stockmatch.corporate.global.earnings.service.EarningsService;
import com.stockmatch.corporate.global.incomestatement.dto.IncomeStatementDto;
import com.stockmatch.corporate.global.incomestatement.service.IncomeStatementService;
import com.stockmatch.corporate.global.news.dto.NewsSentimentDto;
import com.stockmatch.corporate.global.news.service.NewsService;
import com.stockmatch.corporate.global.overview.dto.CompanyOverviewDto;
import com.stockmatch.corporate.global.overview.service.OverviewService;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.member.repository.UserRepository;
import com.stockmatch.user.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.lang.Thread.sleep;


@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final UserRepository userRepository;

    private final OverviewService overviewService;
    private final IncomeStatementService incomeService;
    private final BalancesheetService balancesheetService;
    private final CachflowService cashflowService;
    private final NewsService newsService;
    private final EarningsService earningsService;

    private final BusinessPerformanceMapper businessMapper;
    private final MarketMomentumMapper momentumMapper;
    private final FinancialHealthMapper financialMapper;

    public AnalysisPackage analyzeStock(Long userId, String symbol){
        List<MissingDataItem> missingData = new ArrayList<>();

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        var profile = user.getInvestmentProfile();
        UserContext userContext = UserContext.builder()
                .investmentType(profile != null ? profile.getInvestmentType().name() : "UNDETERMINED")
                .investmentScore(profile != null ? profile.getTotalScore() : 0)
                .currentHoldings(new ArrayList<>()) // 추후 포트폴리오 연동
                .build();

        //데이터 수집
        var overview = safeFetch(()-> overviewService.getCompanyOverview(userId, symbol), "Market", "Overview", missingData);
        sleep(2000);
        var income = safeFetch(()-> incomeService.getIncomeStatement(userId, symbol), "Business", "Income", missingData);
        sleep(2000);
        var balance = safeFetch(()-> balancesheetService.getBalancesheet(userId, symbol), "Health", "BalanceSheet", missingData);
        sleep(2000);
        var cash = safeFetch(()-> cashflowService.getCachflow(userId, symbol), "Health", "CashFlow", missingData);
        sleep(2000);
        var news = safeFetch(()-> newsService.getNewsSentiment(symbol, user), "Market", "News", missingData);
        sleep(2000);
        var earnings = safeFetch(()-> earningsService.getEarnings(userId, symbol), "Market", "Earnings", missingData);

        return AnalysisPackage.builder()
                .user(userContext)
                .businessPerformance(businessMapper.map(income, balance))
                .marketMomentum(momentumMapper.map(overview, news, earnings, symbol))
                .financialHealth(financialMapper.map(balance, cash))
                .missingData(missingData)
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
            missingData.add(new MissingDataItem(section, field, "데이터가 누락되었습니다."));
            return null;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}
