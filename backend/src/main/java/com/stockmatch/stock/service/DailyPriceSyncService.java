package com.stockmatch.stock.service;

import com.stockmatch.stock.client.ExternalDailyPriceClient;
import com.stockmatch.stock.client.kis.KisDailyPriceClient;
import com.stockmatch.stock.domain.DailyPrice;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.repository.DailyPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyPriceSyncService {

    private final DailyPriceRepository dailyPriceRepository;
    private final KisDailyPriceClient kisDailyPriceClient;

    /**
     * 무조건 새 트랜잭션으로 실행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncRange(Security security, LocalDate from, LocalDate to) {
        List<ExternalDailyPriceClient.DailyPriceItem> items =
                kisDailyPriceClient.getDailyPrices(security.getTicker(), from, to);

        for (ExternalDailyPriceClient.DailyPriceItem item : items) {
            DailyPrice entity = dailyPriceRepository.findBySecurityIdAndDate(security.getId(), item.date())
                    .orElse(null);

            if (entity == null) {
                entity = new DailyPrice(
                        null,
                        security,
                        item.date(),
                        item.openPrice(),
                        item.closePrice(),
                        item.highPrice(),
                        item.lowPrice(),
                        item.volume()
                );
            } else {
                entity = new DailyPrice(
                        entity.getId(),
                        security,
                        item.date(),
                        item.openPrice(),
                        item.closePrice(),
                        item.highPrice(),
                        item.lowPrice(),
                        item.volume()
                );
            }

            dailyPriceRepository.save(entity);
        }
    }
}
