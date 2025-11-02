package com.stockmatch.user.service.unlink;

import com.stockmatch.user.domain.AuthProvider;
import com.stockmatch.user.domain.User;

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
