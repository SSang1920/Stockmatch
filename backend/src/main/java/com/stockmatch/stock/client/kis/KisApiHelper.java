package com.stockmatch.stock.client.kis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Slf4j
@Component
public class KisApiHelper {

    private static final int MAX_RETRY = 3;             // 최대 3번 재시도
    private static final long BASE_BACKOFF_MS = 250L;   // 재시도 대기 시간
    private static final long MIN_CALL_GAP_MS = 100L;   // API 호출 최소 간격

    private volatile long lastCallAtMs = 0L;            // 마지막 호출 시간

    /**
     * API 호출을 안전하게 수행 (재시도 + 속도제한 적용)
     */
    public <T> T execute(Callable<T> apiCall, String logPrefix) {
        long backoff = BASE_BACKOFF_MS;

        for (int i = 1; i <= MAX_RETRY; i++) {
            try {
                // 속도 제한
                throttle();
                return apiCall.call();
            } catch (Exception e) {
                log.warn("{} 시도({}) 실패: {}", logPrefix, i, e.getMessage());
            }

            // 실패 시 대기 후 재시도 (마지막 시도가 아닐 경우)
            if (i < MAX_RETRY) {
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                backoff *= 2; // 대기 시간 2배씩 증가 (250ms -> 500ms -> 1s)
            }
        }

        log.error("{} 모든 재시도 실패.", logPrefix);
        return null; // 실패 시 null 반환 (빈 리스트 처리됨)
    }

    /**
     * 속도 제한 로직
     */
    private void throttle() {
        try {
            long now = System.currentTimeMillis();
            long gap = now - lastCallAtMs;

            // 마지막 호출로부터 100ms가 안 지났으면 대기
            if (gap < MIN_CALL_GAP_MS) {
                Thread.sleep(MIN_CALL_GAP_MS - gap);
            }
            // 호출 시간 갱신
            lastCallAtMs = System.currentTimeMillis();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
