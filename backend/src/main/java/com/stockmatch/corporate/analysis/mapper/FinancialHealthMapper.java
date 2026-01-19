package com.stockmatch.corporate.analysis.mapper;

import com.stockmatch.corporate.analysis.dto.sections.FinancialHealth;
import com.stockmatch.corporate.global.balancesheet.dto.BalancesheetDto;
import com.stockmatch.corporate.global.cashflow.dto.CashflowDto;
import org.springframework.stereotype.Component;

@Component
public class FinancialHealthMapper extends BaseMapper {

    public FinancialHealth map (
            BalancesheetDto balanceData,
            CashflowDto cashData){

        if (balanceData == null || balanceData.getAnnualReports() == null || balanceData.getAnnualReports().isEmpty() ||
            cashData ==null || cashData.getAnnualReports() == null || cashData.getAnnualReports().isEmpty()) {
            return null;
        }

        // 최신 리포트 가져오기
        var bsLatest = balanceData.getAnnualReports().get(0);
        var cfLatest = cashData.getAnnualReports().get(0);

        // 재무 상태 지표 계산
        long totalLiabilities = parseLong(bsLatest.getTotalLiabilities());
        long totalEquity = parseLong(bsLatest.getTotalShareholderEquity());
        long totalAssets = parseLong(bsLatest.getTotalAssets());
        long currentAssets = parseLong(bsLatest.getTotalCurrentAssets());
        long currentLiabilities = parseLong(bsLatest.getTotalCurrentLiabilities());

        // 현금 창출 능력 계산
        long operatingCash = parseLong(cfLatest.getOperatingCashflow());
        long capEx = parseLong(cfLatest.getCapitalExpenditures());

        // 잉여현금흐름 = 영업현금흐름 + 자본지출( 만약 capEx가 양수일시 빼기로 수정)
        long freeCashFlow = operatingCash + capEx;

        return FinancialHealth.builder()
                // 부채 및 유동 비율
                .debtToEquityRatio(calculateRatio(totalLiabilities, totalEquity))
                .currentRatio(calculateRatio(currentAssets, currentLiabilities))

                // 현금 흐름 지표
                .operatingCashFlow((double) operatingCash)
                .capitalExpenditures((double) capEx)
                .freeCashFlow((double) freeCashFlow)
                .isCashFlowPositive(freeCashFlow > 0)

                //자금 구조 수치
                .totalAssets((double) totalAssets)
                .totalLiabilities((double) totalLiabilities)
                .build();
    }
}
