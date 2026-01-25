package com.stockmatch.corporate.analysis.mapper.global;

import com.stockmatch.corporate.analysis.dto.components.MarketMomentum;
import com.stockmatch.corporate.analysis.mapper.common.BaseMapper;
import com.stockmatch.corporate.global.earnings.dto.EarningsDto;
import com.stockmatch.corporate.global.news.dto.NewsSentimentDto;
import com.stockmatch.corporate.global.overview.dto.CompanyOverviewDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class GlobalMarketMomentumMapper extends BaseMapper {

    //Alpha Vantage 뉴스 시간 format
    private static final DateTimeFormatter NEWS_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    public MarketMomentum map(
            CompanyOverviewDto overview,
            NewsSentimentDto news,
            EarningsDto earnings,
            String symbol) {

        if (overview == null){
            return null;
        }

        // 뉴스 데이터 처리
        double finalSentimentScore = 0.0;
        String finalNewsSummary = "최근 분석된 요약이 없습니다.";

        if (news != null && news.getFeed() != null && !news.getFeed().isEmpty()){

            List<NewsSentimentDto.Article> articles = news.getFeed();
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

            double totalScore = 0.0;
            int count = 0;
            int summaryCount = 0;
            int maxSummarycount = 5;
            StringBuilder summaryBuilder = new StringBuilder();

            for (var article : articles) {

                // 뉴스가 진짜로 관련이 있는지 검증
                double relevance = 0.0;
                double tickerScore = 0.0;

                if(article.getTickerSentiment() != null ){
                    for (var ts : article.getTickerSentiment()) {
                        if(ts.getTicker().equalsIgnoreCase(symbol)) {
                            relevance = parseDouble(ts.getRelevanceScore());
                            tickerScore = parseDouble(ts.getTickerSentimentScore());
                            break;
                        }
                    }
                }
                LocalDateTime publishedDate = LocalDateTime.parse(article.getTimePublished(), NEWS_DATE_FORMATTER);

                // 한 달 이내의 기사 + 관련도 (0.3이상)만 필터링
                if (relevance >= 0.3 && publishedDate.isAfter(oneMonthAgo)) {

                    if (summaryCount < maxSummarycount) {
                        String rawDate = article.getTimePublished();
                        String publishedAt = rawDate.substring(0, 4) + "-" + rawDate.substring(4, 6) + "-" + rawDate.substring(6, 8);

                        summaryBuilder.append("- ")
                                .append(publishedAt)
                                .append(" | ")
                                .append(article.getTitle())
                                .append("\n");

                        summaryCount++;
                    }
                    totalScore += tickerScore;
                    count++;
                }
            }
            // 한 달 이내의 기사가 0개라면 최근 기사 1개 사용
            if (count >0) {
                finalSentimentScore = totalScore / count;
                finalNewsSummary = summaryBuilder.toString();
            } else {
                var latest = articles.get(0);
                String rawDate = latest.getTimePublished();
                String publishedAt = rawDate.substring(0, 4) + "-" + rawDate.substring(4, 6) + "-" + rawDate.substring(6, 8);

                finalSentimentScore = latest.getOverallSentimentScore();
                finalNewsSummary = String.format("[%s] | %s",
                        publishedAt,
                        latest.getTitle());
            }
        }

        return MarketMomentum.builder()
                .sector(overview.getSector())
                .industry(overview.getIndustry())
                .market(overview.getCountry())

                .peRatio(parseDouble(overview.getPeRatio()))
                .forwardPeRatio(parseDouble(overview.getForwardPe()))
                .psRatio(parseDouble(overview.getPsRatio()))

                // 뉴스 분석 결과
                .newsSentimentScore(finalSentimentScore)
                .newsSummary(finalNewsSummary)

                // 어닝 계산
                .lastEarningsSurpriseRatio(calculateEarningsSurprise(earnings))
                .build();
    }

    private Double calculateEarningsSurprise(EarningsDto earnings) {
        if(earnings == null || earnings.getQuarterlyEarnings() == null || earnings.getQuarterlyEarnings().isEmpty()) {
            return 0.0;
        }

        var latest = earnings.getQuarterlyEarnings().get(0);
        Double reported = parseDouble(latest.getReportedEPS());
        Double estimate = parseDouble(latest.getEstimatedEPS());

        if (estimate == null || estimate == 0){
            return 0.0;
        }

        return (reported - estimate) / Math.abs(estimate);
    }
}
