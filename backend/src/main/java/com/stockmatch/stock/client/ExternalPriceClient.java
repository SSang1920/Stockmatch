package com.stockmatch.stock.client;

import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.dto.StockPriceResponse;

public interface ExternalPriceClient {
    StockPriceResponse getRealtime(String region, String ticker);

    Security fetchCompanyProfile(String ticker, String region);
}
