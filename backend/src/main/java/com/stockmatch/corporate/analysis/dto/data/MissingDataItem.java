package com.stockmatch.corporate.analysis.dto.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MissingDataItem {
    private String section;
    private String field;
    private String reason;
}
