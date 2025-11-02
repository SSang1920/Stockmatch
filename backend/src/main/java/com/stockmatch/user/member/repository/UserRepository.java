package com.stockmatch.user.member.repository;

import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.member.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

}
