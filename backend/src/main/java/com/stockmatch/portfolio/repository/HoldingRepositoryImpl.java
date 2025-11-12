package com.stockmatch.portfolio.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.stockmatch.portfolio.domain.Holding;
import com.stockmatch.portfolio.domain.QHolding;
import com.stockmatch.stock.domain.QSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class HoldingRepositoryImpl implements HoldingRepositoryCustom {

    private final JPAQueryFactory query;

    @Override
    public List<Holding> findAllWithSecurityByPortfolioId(Long portfolioId) {
        QHolding h = QHolding.holding;
        QSecurity s = QSecurity.security;

        return query.selectFrom(h)
                .join(h.security, s).fetchJoin()
                .where(h.portfolio.id.eq(portfolioId))
                .fetch();
    }
}
