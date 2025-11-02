package com.stockmatch.user.auth.service.unlink;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.member.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleUnlink implements OAuthUnlinker {

    private final RestTemplate restTemplate;

    private static final String GOOGLE_UNLINK_URL = "https://oauth2.googleapis.com/revoke";

    @Override
    public AuthProvider getProvider(){
        return AuthProvider.GOOGLE;
    }

    @Override
    public void unlink(User user) {
        String refreshToken = user.getRefreshToken();

        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, null);
        String response = restTemplate.postForObject(GOOGLE_UNLINK_URL, request, String.class);
        log.info("Google unlink response: {}", response);

    }
}
