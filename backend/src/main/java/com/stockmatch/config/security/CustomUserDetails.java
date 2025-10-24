package com.stockmatch.config.security;

import com.stockmatch.user.domain.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;


@Getter
public class CustomUserDetails implements UserDetails, OAuth2User {

    private final User user;
    private Map<String, Object> attributes;

    public CustomUserDetails(User user){
        this.user = user;
    }

    public CustomUserDetails(User user, Map<String, Object> attributes){
        this.user = user;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey()));
    }

    @Override
    public String getPassword(){
        return null;
    }

    @Override
    public String getUsername() {
        return String.valueOf(user.getId());
    }

    @Override
    public Map<String, Object> getAttributes(){
        return this.attributes;
    }

    @Override
    public String getName(){
        return String.valueOf(user.getId());
    }

    @Override
    public boolean isAccountNonExpired(){
        return  true;
    }

    @Override
    public boolean isAccountNonLocked(){
        return  true;
    }

    @Override
    public boolean isCredentialsNonExpired(){
        return  true;
    }
    @Override
    public boolean isEnabled(){
        return  true;
    }

}
