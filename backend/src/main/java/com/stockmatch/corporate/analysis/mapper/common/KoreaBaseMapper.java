package com.stockmatch.corporate.analysis.mapper.common;
import com.stockmatch.corporate.korea.finance.dto.DartFinancialRawResponse.RawAccountItem;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class KoreaBaseMapper extends  BaseMapper{

    protected Long findValue(List<RawAccountItem> items, String id, String nameKeyword) {
       if (items ==null || items.isEmpty()) {
           return null;
       }

       RawAccountItem byId = items.stream()
               .filter(item -> id != null && id.equals(item.getAccountId()))
               .findFirst()
               .orElse(null);

       if (byId != null) {
           log.debug("[DART] matchedBy=ID targetId={} targetName={} pickedId={} pickedName={} amount={}",
                   id, nameKeyword, byId.getAccountId(), byId.getAccountNm(), byId.getThstrmAmount());
           return parseAmount(byId.getThstrmAmount());
       }

       String keyword = nameKeyword == null ? null : nameKeyword.replace(" ", "");
       RawAccountItem byName = items.stream()
               .filter(item -> item.getAccountNm() != null && item.getAccountNm().replace(" ","").equals(keyword))
               .findFirst()
               .orElse(null);

       return byName == null ? null : parseAmount(byName.getThstrmAmount());
    }

    private Long parseAmount(String amount) {
        if (amount == null || amount.isBlank() || amount.equals("-")) {
            return 0L;
        }
        try{
            return Long.parseLong(amount.replace(",",""));
        } catch (Exception e) {
            return 0L;
        }
    }
}
