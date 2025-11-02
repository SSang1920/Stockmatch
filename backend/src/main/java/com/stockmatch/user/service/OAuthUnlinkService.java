package com.stockmatch.user.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.user.domain.AuthProvider;
import com.stockmatch.user.domain.User;
import com.stockmatch.user.service.unlink.OAuthUnlinker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;


import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthUnlinkService {

    private final Map<AuthProvider, OAuthUnlinker> unlinkerMap;

    public OAuthUnlinkService(List<OAuthUnlinker> unlinkers){
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
