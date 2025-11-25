package com.stockmatch.stock.importer;

import com.stockmatch.stock.domain.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImportFileRepository extends JpaRepository<ImportFile, Long> {

    Optional<ImportFile> findByLocation(String location);

    Optional<ImportFile> findByLocationAndExchange(String location, Exchange exchange);
}
