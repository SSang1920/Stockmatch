package com.stockmatch.user.auth.dto;

import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.member.domain.User;
import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class OAuthAttributes {

    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String profileImageUrl;
    private AuthProvider provider;
    private String providerId;

    //모든 필드를 받는 생성자
    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String profileImageUrl, AuthProvider provider, String providerId){
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.provider = provider;
        this.providerId = providerId;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("google".equals(registrationId)){
            return ofGoogle("id",attributes);
        }
        if ("naver".equals(registrationId)){
            return ofNaver("id", attributes);
        }
        if ("kakao".equals(registrationId)){
            return ofKakao("id", attributes);
        }

        throw new IllegalArgumentException("Unsupported social login: " + registrationId);
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {

        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuthAttributes.builder()
                .name((String) response.get("name"))
                .email((String) response.get("email"))
                .profileImageUrl((String) response.get("profile_image"))
                .provider(AuthProvider.NAVER)
                .providerId((String) response.get(userNameAttributeName))
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {

        // 'properties' Map에서 닉네임, 프로필 이미지 추출
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        String nickname = (String) properties.get("nickname");
        String profileImageUrl = (String) properties.get("profile_image");

        // 'kakao_account' Map에서 이메일 추출
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        String email = (kakaoAccount != null) ? (String) kakaoAccount.get("email") : null;

        return OAuthAttributes.builder()
                .name(nickname)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .provider(AuthProvider.KAKAO)
                .providerId(String.valueOf(attributes.get("id")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes){
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .profileImageUrl((String) attributes.get("picture"))
                .provider(AuthProvider.GOOGLE)
                .providerId(String.valueOf(attributes.get(userNameAttributeName)))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    public User toEntity(){
        return User.builder()
                .name(name)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .provider(provider)
                .providerId(providerId)
                .build();
    }

}
