package com.stockmatch.corporate.global.news.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stockmatch.corporate.common.dto.CacheableData;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class NewsSentimentDto implements CacheableData {

    private String items;

    @JsonProperty("sentiment_score_definition")
    private String sentimentScoreDefinition;

    @JsonProperty("relevance_score_definition")
    private String relevanceScoreDefinition;

    @JsonProperty("feed")
    private List<Article> feed;

    @Override
    public boolean isValidForCaching(){
        return feed != null && !feed.isEmpty();
    }

    @Getter
    @NoArgsConstructor
    public static class Article {
        private String title;
        private String url;

        @JsonProperty("time_published")
        private String timePublished;

        private List<String> authors;
        private String summary;

        @JsonProperty("banner_image")
        private String bannerImage;

        private String source;

        @JsonProperty("category_within_source")
        private String categoryWithinSource;

        @JsonProperty("source_domain")
        private String sourceDomain;

        private List<Topic> topics;

        @JsonProperty("overall_sentiment_score")
        private double overallSentimentScore;

        @JsonProperty("overall_sentiment_label")
        private String overallSentimentLabel;

        @JsonProperty("ticker_sentiment")
        private List<TickerSentiment> tickerSentiment;
    }

    @Getter
    @NoArgsConstructor
    public static class Topic{
        private String topic;

        @JsonProperty("relevance_score")
        private String relevanceScore;
    }

    @Getter
    @NoArgsConstructor
    public static class TickerSentiment{
        private String ticker;

        @JsonProperty("relevance_score")
        private String relevanceScore;

        @JsonProperty("ticker_sentiment_score")
        private String tickerSentimentScore;

        @JsonProperty("ticker_sentiment_label")
        private String tickerSentimentLabel;
    }
}
