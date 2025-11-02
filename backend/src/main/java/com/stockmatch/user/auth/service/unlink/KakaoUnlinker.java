package com.stockmatch.user.auth.service.unlink;

import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.member.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoUnlinker implements OAuthUnlinker {

    private final RestTemplate RestTemplate;

    @Value("${social.kakao.admin-key}")
    private String kakaoAdminKey;

    private static final String KAKAO_UNLINK_URL ="https://kapi.kakao.com/v1/user/unlink";

    @Override
    public AuthProvider getProvider(){
        return AuthProvider.KAKAO;
    }

    @Override
    public void unlink(User user){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Authorization", "KakaoAK " + kakaoAdminKey);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("target_id_type", "user_id");
        formData.add("target_id", user.getProviderId());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
        String response = RestTemplate.postForObject(KAKAO_UNLINK_URL, request, String.class);
        log.info("kakao unlink response : {}", response);
    }


}
