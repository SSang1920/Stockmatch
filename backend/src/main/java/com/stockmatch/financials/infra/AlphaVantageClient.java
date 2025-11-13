package com.stockmatch.financials.infra;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.financials.client.ExternalFinancialsClient;
import com.stockmatch.financials.dto.CompanyOverviewResponse;
import com.stockmatch.user.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Component("alphaVantageFinancialsClient")
@RequiredArgsConstructor
public class AlphaVantageClient implements ExternalFinancialsClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${alphavantage.base-url}")
    private String baseUrl;

   @Override
   public CompanyOverviewResponse getCompanyOverview(String symbol, String apiKey){

       URI uri = UriComponentsBuilder
               .fromUriString(baseUrl)
               .path("/query")
               .queryParam("function", "OVERVIEW")
               .queryParam("symbol", symbol)
               .queryParam("apikey", apiKey)
               .build(true).toUri();

       log.info("Requesting Company Overview from Alpha Vantage for symbol: {}", symbol);

       Map<String, Object> rawResponse = restTemplate.getForObject(uri, Map.class);

       if (rawResponse == null || rawResponse.isEmpty()) {

           return null;
       }

       return objectMapper.convertValue(rawResponse, CompanyOverviewResponse.class);
   }
}
