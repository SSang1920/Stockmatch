package com.stockmatch.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardResponseDto {

    private Long dailyActiveUsers; //일일 유저 수
    private Long newJoinCount; //신규 가입한 유저 수
    private Long withdrawnCount; // 탈퇴한 유저 수

    private Long dailyActivePortfolios; // 오늘 포트폴리오를 수정한 유저의 수

    private Long kisApiErrorCount; // KIS API 호출 건수 초과 수
}
