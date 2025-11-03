package com.stockmatch.stock.repository;

import com.stockmatch.stock.domain.Security;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecurityRepository extends JpaRepository<Security, Long> {


    Optional<Security> findByTicker(String ticker);
}
