package com.stockmatch.stock.client;

import com.stockmatch.stock.client.kis.dto.MinutePriceItem;

import java.util.List;

public interface ExternalMinutePriceClient {

    List<MinutePriceItem> getMinutePrices(String ticker);
}
