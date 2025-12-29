package com.stockmatch.stock.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.stockmatch.stock.domain.Security;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.stockmatch.stock.domain.QSecurity.security;

@RequiredArgsConstructor
public class SecurityRepositoryImpl implements SecurityRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Security> searchByKeyword(String keyword, Pageable pageable) {
        return queryFactory
                .selectFrom(security)
                .where(
                        containsTicker(keyword)
                        .or(containsName(keyword))
                        .or(containsEnglishName(keyword))
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private BooleanExpression containsTicker(String keyword) {
        return keyword != null ? security.ticker.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression containsName(String keyword) {
        return keyword != null ? security.name.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression containsEnglishName(String keyword) {
        return keyword != null ? security.englishName.containsIgnoreCase(keyword) : null;
    }
}
