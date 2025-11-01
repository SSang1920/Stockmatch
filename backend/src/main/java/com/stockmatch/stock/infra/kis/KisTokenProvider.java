package com.stockmatch.stock.infra.kis;

import com.stockmatch.stock.dto.KisAuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KisTokenProvider {

    private final KisAuthClient kisAuthClient;

    // 메모리에 들고 있을 값
    private String accessToken;
    private long expiresAtEpoch = 0L;   // 만료 시각

    public synchronized String getAccessToken() {
        long now = System.currentTimeMillis() / 1000;

        // 처음이거나 만료가 임박하면 다시 발급
        if (accessToken == null || now > expiresAtEpoch - 60) {
            KisAuthResponse response = kisAuthClient.getAccessToken();
            this.accessToken = response.getAccessToken();

            // KIS 응답에 expire 정보가 있으면 그 값으로 계산
            // 없으면 24시간으로 계산
            long expiresIn = response.getExpiresIn() != null ? response.getExpiresIn() : 60 * 60 * 24;
            this.expiresAtEpoch = now + expiresIn;
        }

        return this.accessToken;
    }
}
