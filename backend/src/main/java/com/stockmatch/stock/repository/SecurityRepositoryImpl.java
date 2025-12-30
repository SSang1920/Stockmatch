package com.stockmatch.stock.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
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

        // 정렬 순서 로직
        // 1순위: 티커가 정확히 일치
        // 2순위: 티커가 검색어로 시작
        // 3순위: 그 외
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(security.ticker.equalsIgnoreCase(keyword)).then(1)
                .when(security.ticker.startsWithIgnoreCase(keyword)).then(2)
                .otherwise(3);

        return queryFactory
                .selectFrom(security)
                .where(
                        containsTicker(keyword)
                        .or(containsName(keyword))
                        .or(containsEnglishName(keyword))
                )
                .orderBy(rankPath.asc())
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
