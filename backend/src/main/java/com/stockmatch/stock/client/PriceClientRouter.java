package com.stockmatch.stock.client;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.dto.StockPriceResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Component
public class PriceClientRouter {

    private final ExternalPriceClient usClient;
    private final ExternalPriceClient krClient;

    public PriceClientRouter(
        @Qualifier("finnhubClient") ExternalPriceClient usClient,
        @Qualifier("kisStockClient") ExternalPriceClient krClient
    ) {
        this.usClient = usClient;
        this.krClient = krClient;
    }

    public StockPriceResponse getRealtime(String region, String ticker) {
        if ("US".equalsIgnoreCase(region)) {
            return usClient.getRealtime(region, ticker);
        }

        if ("KR".equalsIgnoreCase(region)) {
            return krClient.getRealtime(region, ticker);
        }

        throw new BusinessException(ErrorCode.UNSUPPORTED_REGION);
    }
}
