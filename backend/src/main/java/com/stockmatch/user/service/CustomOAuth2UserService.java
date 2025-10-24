package com.stockmatch.user.service;

import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.user.domain.User;
import com.stockmatch.user.dto.OAuthAttributes;
import com.stockmatch.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException{

        // 사용자 정보를 가져와 로드
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 어디 소셜인지 구분
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 사용자의 고유 ID를 가져오는 기준이 되는 키 값을 가져옴 (Google : sub, naver :id, kakao :id)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 받아온 사용자 정보 OAuthAttributes 객체로 변환
        OAuthAttributes attributes = OAuthAttributes.of(
                registrationId,
                userNameAttributeName,
                oAuth2User.getAttributes()
        );

        User user = saveOrUpdate(attributes);

        return new CustomUserDetails(user, attributes.getAttributes());
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByProviderAndProviderId(
                attributes.getProvider(),
                attributes.getProviderId()
        )
                //DB에 사용자가 이미 있는 경우 , 이름과 프로필 사진 업데이트
                .map(entity -> entity.updateOAuthInfo(attributes.getName(), attributes.getProfileImageUrl() ))
                //DB에 사용자가 없는 경우  OAuthAttributes -> User 엔티티로 변환 후 생성
                .orElse(attributes.toEntity());

        return userRepository.save(user);
    }
}
