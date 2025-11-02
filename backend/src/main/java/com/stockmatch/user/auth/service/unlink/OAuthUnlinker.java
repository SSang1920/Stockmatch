package com.stockmatch.user.auth.service.unlink;

import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.member.domain.User;

public interface OAuthUnlinker {

    /**
     * Unlinker가 담당하는 소셜 로그인 제공자 반환
     */
    AuthProvider getProvider();

    /**
     * 실제 연결 끈히 로직 수행
     */
    void unlink(User user);
}
