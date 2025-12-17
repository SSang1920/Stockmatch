package com.stockmatch.corporate.news.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.corporate.news.dto.NewsSentimentDto;
import com.stockmatch.corporate.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/corporate/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public ApiResponse<NewsSentimentDto> getNews(
            @RequestParam(required = false) String tickers,
            @RequestParam(required = false) String topics,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        NewsSentimentDto response = newsService.getNewsSentiment(tickers, userDetails.getUser());
        
        return ApiResponse.ok(response);
    }
}
