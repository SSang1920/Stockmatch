package com.stockmatch.user.dto;

import com.stockmatch.user.domain.AlphaVantageKey;
import com.stockmatch.user.domain.AuthProvider;
import com.stockmatch.user.domain.User;
import com.stockmatch.user.domain.UserInvestmentType;
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
