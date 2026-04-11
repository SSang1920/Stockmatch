package com.stockmatch.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.stockmatch.portfolio.domain.Currency;
import com.stockmatch.portfolio.domain.TradeType;
import com.stockmatch.portfolio.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(

        Long id,
        Long portfolioId,
        Long securityId,
        String ticker,
        String name,
        Currency Currency,
        TradeType type,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fee,
        BigDecimal totalAmount,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime tradeAt,
        String memo
) {

    public static TransactionResponse from(Transaction transaction) {
        // 총 거래 금액 계산 (수량 * 단가)
        BigDecimal total = transaction.getQuantity().multiply(transaction.getPrice());

        return new TransactionResponse(
                transaction.getId(),
                transaction.getPortfolio().getId(),
                transaction.getSecurity().getId(),
                transaction.getSecurity().getTicker(),
                transaction.getSecurity().getName(),
                transaction.getSecurity().getCurrency(),
                transaction.getType(),
                transaction.getQuantity(),
                transaction.getPrice(),
                normalizeFee(transaction.getFee()),
                total,
                transaction.getTradeAt(),
                transaction.getMemo()
        );
    }

    private static BigDecimal normalizeFee(BigDecimal fee) {
        if (fee == null) {
            return java.math.BigDecimal.ZERO;
        }
        return fee.stripTrailingZeros();
    }
}
