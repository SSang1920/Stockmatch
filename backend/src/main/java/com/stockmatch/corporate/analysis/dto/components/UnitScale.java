package com.stockmatch.corporate.analysis.dto.components;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UnitScale {
    RAW("원천 수치"),
    THOUSAND("천 단위"),
    MILLION("백만 단위");

    private final String description;
}
