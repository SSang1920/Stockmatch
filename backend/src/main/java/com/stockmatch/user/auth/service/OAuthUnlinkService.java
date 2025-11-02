package com.stockmatch.user.auth.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.auth.service.unlink.OAuthUnlinker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;


import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OAuthUnlinkService {

    private final List<OAuthUnlinker> unlinkers;
    private Map<AuthProvider, OAuthUnlinker> unlinkerMap;

    @PostConstruct
    public void initialize() {
        this.unlinkerMap = unlinkers.stream().collect(
                Collectors.toMap(OAuthUnlinker::getProvider, Function.identity())
        );
    }

    public void unlink(User user) {
        AuthProvider provider = user.getProvider();

        if(provider == null){
            return;
        }

        OAuthUnlinker unlinker = unlinkerMap.get(provider); // 해당하는 provider unlinker 연결

        if(unlinker != null){
            try{
                unlinker.unlink(user);
            } catch (RestClientException e){
                throw new BusinessException(ErrorCode.OAUTH_UNLINK_FAILED);
            }
        }
    }
}
