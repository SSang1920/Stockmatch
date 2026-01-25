package com.stockmatch.corporate.analysis.mapper.korea;

import com.stockmatch.corporate.analysis.dto.components.UnitScale;
import com.stockmatch.corporate.analysis.dto.components.FinancialHealth;
import com.stockmatch.corporate.analysis.mapper.common.KoreaBaseMapper;
import com.stockmatch.corporate.korea.finance.dto.DartFinancialRawResponse.RawAccountItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KoreaFinancialHealthMapper extends KoreaBaseMapper {

    public FinancialHealth map(List<RawAccountItem> items) {

        if (items == null || items.isEmpty()) {
            return FinancialHealth.builder().build();
        }

        // 재무상태표 주요 항목 추출
        Long totalAssets = findValue(items, "ifrs-full_Assets", "자산총계");
        Long totalLiabilities = findValue(items, "ifrs-full_Liabilities", "부채총계");
        Long totalEquity = findValue(items, "ifrs-full_Equity", "자본총계");

        Long currentAssets = findValue(items, "ifrs-full_CurrentAssets", "유동자산");
        Long currentLiabilities = findValue(items, "ifrs-full_CurrentLiabilities", "유동부채");
        Long cashAndEquivalents = findValue(items, "ifrs-full_CashAndCashEquivalents", "현금및현금성자산");

        // 현금흐름표 주요 항목 추출
        Long operatingCashObj = findValue(items, "ifrs-full_CashFlowsFromUsedInOperatingActivities", "영업활동현금흐름");
        // CapEx (자본지출)
        Long capExObj = findValue(items,"ifrs-full_PurchaseOfPropertyPlantAndEquipment", "유형자산의취득");

        //NPE 방지를 위한 Null-safe변환
        long operatingCash = operatingCashObj != null ? operatingCashObj : 0L;
        long capEx = capExObj != null ? capExObj : 0L;

        long capExOutflow = capEx > 0 ? -capEx : capEx;

        // 잉여현금흐름 = 영업현금 + CapEx
        //보통 음수
        long freeCashFlow = operatingCash + capExOutflow;

        return FinancialHealth.builder()
                .currency("KRW")
                .unit(UnitScale.RAW)
                //안정성 지표
                .debtToEquityRatio(calculateRatio(totalLiabilities, totalEquity))
                .currentRatio(calculateRatio(currentAssets, currentLiabilities))

                // 현금 흐름 지표
                .operatingCashFlow((double) operatingCash)
                .capitalExpenditures((double) Math.abs(capEx))
                .freeCashFlow((double) freeCashFlow)
                .isCashFlowPositive(freeCashFlow > 0)

                //규모(자산/ 부채)
                .totalAssets((double) totalAssets)
                .totalLiabilities((double) totalLiabilities)
                .cashAndEquivalents((double) cashAndEquivalents)
                .build();
    }
}
