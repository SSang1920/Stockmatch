package com.stockmatch.corporate.analysis.mapper;

import com.stockmatch.corporate.analysis.dto.sections.BusinessPerformance;
import com.stockmatch.corporate.global.balancesheet.dto.BalancesheetDto;
import com.stockmatch.corporate.global.incomestatement.dto.IncomeStatementDto;
import org.springframework.stereotype.Component;

@Component
public class BusinessPerformanceMapper extends BaseMapper{

    public BusinessPerformance map(
            IncomeStatementDto incomeData,
            BalancesheetDto balanceData) {

        if (incomeData == null || incomeData.getAnnualReports() == null || incomeData.getAnnualReports().isEmpty()){
            return null;
        }

        var reports = incomeData.getAnnualReports();
        var latest = reports.get(0); //최신 연도
        var previous = reports.size() > 1 ? reports.get(1) : null; //전년도

        // 문자열 -> 숫자 변환
        Long latestRev = parseLong(latest.getTotalRevenue());
        Long latestOpInc = parseLong(latest.getOperatingIncome());
        Long latestNetInc = parseLong(latest.getNetIncome());

        // 자기 자본 이익률 계산
        Double roe = null;
        if (balanceData != null && balanceData.getAnnualReports() != null && !balanceData.getAnnualReports().isEmpty()) {
            var bsLatest = balanceData.getAnnualReports().get(0);
            Long totalEquity = parseLong(bsLatest.getTotalShareholderEquity());
            roe = calculateRatio(latestNetInc, totalEquity); // 순이익 / 자기자본
        }

        // 수익성 계산
        double operatingMarginRatio = calculateRatio(latestOpInc, latestRev);
        double netProfitMarginRatio = calculateRatio(latestNetInc, latestRev);

        // 성장성 계산
        Double revGrowth = null;
        Double opGrowth = null;

        if (previous != null) {

            Long prevRev = parseLong(previous.getTotalRevenue());
            Long prevOpInc = parseLong(previous.getOperatingIncome());

            revGrowth = calculateGrowth(latestRev, prevRev);
            opGrowth = calculateGrowth(latestOpInc, prevOpInc);
        }

        return BusinessPerformance.builder()
                .asOfDate(latest.getFiscalDateEnding())
                .operatingMarginRatio(operatingMarginRatio)
                .netProfitMarginRatio(netProfitMarginRatio)
                .roeRatio(roe)
                .revenueGrowthRate(revGrowth)
                .operatingIncomeGrowthRate(opGrowth)
                .build();
    }
}
