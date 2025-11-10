package com.stockmatch.stock.importer;

import com.stockmatch.stock.domain.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KrxImportFileRepository extends JpaRepository<KrxImportFile, Long> {

    Optional<KrxImportFile> findByLocation(String location);

    Optional<KrxImportFile> findByLocationAndExchange(String location, Exchange exchange);
}
