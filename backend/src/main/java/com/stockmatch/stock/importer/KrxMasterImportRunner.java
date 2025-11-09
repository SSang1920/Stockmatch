package com.stockmatch.stock.importer;

import com.stockmatch.stock.domain.Market;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "import.krx", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class KrxMasterImportRunner implements CommandLineRunner {

    private final KrxMasterLoader loader;
    private final SecurityRepository securityRepository;

    @Value("${import.krx.kospi-location:classpath:import/kospi_code.csv}")
    private String kospiLocation;

    @Value("${import.krx.kosdaq-location:classpath:import/kosdaq_code.csv}")
    private String kosdaqLocation;

    @Value("${import.krx.load-kospi:true}")
    private boolean loadKospi;

    @Value("${import.krx.load-kosdaq:true}")
    private boolean loadKosdaq;

    @Value("${import.krx.encoding:UTF-8}")
    private String encoding;

    @Override
    public void run(String... args) throws Exception {
        var charset = toCharset(encoding);

        loader.loadKospi(kospiLocation, charset);
        loader.loadKosdaq(kosdaqLocation, charset);

        // ===== 간단 검증 로그 =====
        long korCount = securityRepository.countByMarket(Market.KOR);
        log.info("[VERIFY] security KOR rows = {}", korCount);

        securityRepository.findByTickerAndMarket("000660", Market.KOR)
                .ifPresentOrElse(
                        s -> log.info("[VERIFY] 000660 loaded: name='{}', isin='{}'", s.getName(), s.getIsin()),
                        () -> log.warn("[VERIFY] 000660 not found")
                );
    }

    private Charset toCharset(String name) {
        if (name == null || name.isBlank()) return StandardCharsets.UTF_8;
        var n = name.trim().toUpperCase();
        if (n.equals("CP949") || n.equals("MS949") || n.equals("EUC-KR")) return Charset.forName("MS949");
        if (n.equals("UTF8") || n.equals("UTF-8")) return StandardCharsets.UTF_8;
        return Charset.forName(name);
    }
}
