package com.stockmatch.corporate.analysis.mapper.korea;

import com.stockmatch.corporate.analysis.dto.components.BusinessPerformance;
import com.stockmatch.corporate.analysis.mapper.common.KoreaBaseMapper;
import com.stockmatch.corporate.korea.finance.dto.DartFinancialRawResponse.RawAccountItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KoreaBusinessPerformanceMapper extends KoreaBaseMapper {

    // 수익성 / 성장성
    public BusinessPerformance map(
            String year,
            List<RawAccountItem> currentItems,
            List<RawAccountItem> prevItems
            ){

        // 주요 수치
        Long revenue = findValue(currentItems, "ifrs-full_Revenue", "매출액");
        Long opIncome = findValue(currentItems, "dart_OperatingIncomeLoss", "영업이익");
        Long netIncome = findValue(currentItems, "ifrs-full_ProfitLoss", "당기순이익");

        // 자본총계(ROE)
        Long totalEquity = findValue(currentItems, "ifrs-full_Equity", "자본총계");

        // 전년도 데이터 추출
        Long prevRevenue = null;
        Long prevOpIncome = null;

        if (prevItems != null && !prevItems.isEmpty()) {
            prevRevenue = findValue(prevItems, "ifrs-full_Revenue", "매출액");
            prevOpIncome = findValue(prevItems, "dart_OperatingIncomeLoss", "영업이익");
        }

        return BusinessPerformance.builder()
                .asOfDate(year + ".12 (FY)")
                .operatingMarginRatio(calculateRatio(opIncome, revenue))
                .netProfitMarginRatio(calculateRatio(netIncome, revenue))
                .roeRatio(calculateRatio(netIncome, totalEquity))
                .revenueGrowthRate(calculateGrowth(revenue, prevRevenue))
                .operatingIncomeGrowthRate(calculateGrowthWithAbs(opIncome, prevOpIncome))
                .build();
    }


}
