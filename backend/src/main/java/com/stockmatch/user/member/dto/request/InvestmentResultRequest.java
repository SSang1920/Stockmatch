package com.stockmatch.user.member.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InvestmentResultRequest {

    @NotNull
    @Min(0)
    private Integer totalScore;

    private String rawAnswers;
}
