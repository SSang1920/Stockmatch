package com.stockmatch.corporate.analysis.mapper.global;

import com.stockmatch.corporate.analysis.dto.component.BusinessPerformance;
import com.stockmatch.corporate.analysis.mapper.common.BaseMapper;
import com.stockmatch.corporate.global.balancesheet.dto.BalancesheetDto;
import com.stockmatch.corporate.global.incomestatement.dto.IncomeStatementDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class GlobalBusinessPerformanceMapper extends BaseMapper {

    public BusinessPerformance map(
            IncomeStatementDto incomeData,
            BalancesheetDto balanceData) {

        if (incomeData == null || incomeData.getAnnualReports() == null || incomeData.getAnnualReports().isEmpty()){
            return null;
        }

        List<IncomeStatementDto.AnnualReports> reports = new ArrayList<>(incomeData.getAnnualReports());
        reports.sort(Comparator.comparing(IncomeStatementDto.AnnualReports::getFiscalDateEnding).reversed());

        var latest = reports.get(0); //최신 연도
        var previous = reports.size() > 1 ? reports.get(1) : null; //전년도

        // 문자열 -> 숫자 변환
        long latestRev = parseLong(latest.getTotalRevenue());
        long latestOpInc = parseLong(latest.getOperatingIncome());
        long latestNetInc = parseLong(latest.getNetIncome());

        // 자기 자본 이익률 계산
        Double roe = null;
        if (balanceData != null && balanceData.getAnnualReports() != null && !balanceData.getAnnualReports().isEmpty()) {
            var bsLatest = balanceData.getAnnualReports().get(0);
            long totalEquity = parseLong(bsLatest.getTotalShareholderEquity());
            roe = calculateRatio(latestNetInc, totalEquity); // 순이익 / 자기자본
        }

        // 수익성 계산
        double operatingMarginRatio = calculateRatio(latestOpInc, latestRev);
        double netProfitMarginRatio = calculateRatio(latestNetInc, latestRev);

        // 성장성 계산
        Double revGrowth = null;
        Double opGrowth = null;

        if (previous != null) {
            long prevRev = parseLong(previous.getTotalRevenue());
            long prevOpInc = parseLong(previous.getOperatingIncome());

            revGrowth = calculateGrowth(latestRev, prevRev);
            opGrowth = calculateGrowthWithAbs(latestOpInc, prevOpInc);
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
