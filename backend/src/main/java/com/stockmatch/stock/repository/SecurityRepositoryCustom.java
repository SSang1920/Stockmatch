package com.stockmatch.stock.repository;

import com.stockmatch.stock.domain.Security;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SecurityRepositoryCustom {

    List<Security> searchByKeyword(String keyword, Pageable pageable);
}
