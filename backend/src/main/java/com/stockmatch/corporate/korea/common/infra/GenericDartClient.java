package com.stockmatch.corporate.korea.common.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GenericDartClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${dart.api.key}")
    private String apiKey;

    @Value("${dart.api.base-url}")
    private String baseUrl;

    /**
     * DART JSON API 요청 공통메소드
     */
    public <T> T fetchJsonData(String path, Map<String, String> params, Class<T> dtoClass){
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl).path(path)
                .queryParam("crtfc_key", apiKey);

        if (params != null) {
            params.forEach(builder::queryParam);
        }

        String url = builder.build().toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response ==null || "013".equals(response.get("status"))) {
                throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
            }

            return objectMapper.convertValue(response, dtoClass);
        } catch (Exception e){
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * DART 고유 번호 리스트용 데이터 (Zip/XML) 요청 메소드
     */
    public byte[] fetchRawData(String path) {
        String url = UriComponentsBuilder.fromUriString(baseUrl).path(path)
                .queryParam("crtfc_key", apiKey)
                .build().toUriString();

        return restTemplate.getForObject(url, byte[].class);
    }
}
