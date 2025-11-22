package com.stockmatch.corporate.common.dto;

public interface CacheableData {

    /**
     * 받은 값이 유효한지 확인
     * @return 유효 true, else false
     */
    boolean isValidForCaching();
}
