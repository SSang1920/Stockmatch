package com.stockmatch.corporate.korea.common.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DartCorpCodeClient {

    private final GenericDartClient genericDartClient;

    /**
     * DART로부터 고유번호 리스트 데이터 가져오기
     */
    public byte[] getCorpCodeList() {
        return genericDartClient.fetchRawData("/corpCode.xml");
    }


}
