package com.stockmatch.user.member.dto.response;

import com.stockmatch.user.member.domain.AlphaVantageKey;
import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.member.domain.enums.UserInvestmentType;
import lombok.Getter;

@Getter
public class UserInfoResponse {

    private final Long id;
    private final String email;
    private final String name;
    private final String profileImageUrl;
    private final AuthProvider authprovider;
    private final UserInvestmentType investmentType;
    private final String apiKey;

    public UserInfoResponse(User user, AlphaVantageKey alphaVantageKey){
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.profileImageUrl = user.getProfileImageUrl();
        this.authprovider = user.getProvider();
        this.investmentType = user.getInvestmentType();
        this.apiKey = (alphaVantageKey != null) ? alphaVantageKey.getKeyCipher() : null;
    }



}
