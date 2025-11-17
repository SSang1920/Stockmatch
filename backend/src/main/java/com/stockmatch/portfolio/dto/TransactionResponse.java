package com.stockmatch.portfolio.dto;

import com.stockmatch.portfolio.domain.TradeType;
import com.stockmatch.portfolio.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(

        Long id,
        Long portfolioId,
        Long securityId,
        TradeType type,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fee,
        LocalDateTime tradeAt,
        String memo
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getPortfolio().getId(),
                transaction.getSecurity().getId(),
                transaction.getType(),
                transaction.getQuantity(),
                transaction.getPrice(),
                normalizeFee(transaction.getFee()),
                transaction.getTradeAt(),
                transaction.getMemo()
        );
    }

    private static BigDecimal normalizeFee(BigDecimal fee) {
        if (fee == null) {
            return BigDecimal.ZERO;
        }
        return fee.stripTrailingZeros();
    }
}
