package com.stockmatch.stock.importer;

import com.stockmatch.stock.domain.Exchange;
import com.stockmatch.stock.domain.Market;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.stockmatch.stock.importer.ImportChecksumUtils.sha256Hex;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "import.krx", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class KrxMasterImportRunner implements CommandLineRunner {

    private final KrxMasterLoader loader;
    private final SecurityRepository securityRepository;
    private final KrxImportFileRepository fileRepository;
    private final ResourceLoader resourceLoader;

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

        if (loadKospi) {
            importIfChanged(kospiLocation, Exchange.KOSPI, charset);
        } else {
            log.info("[KOSPI] disabled by config.");
        }

        if (loadKosdaq) {
            importIfChanged(kosdaqLocation, Exchange.KOSDAQ, charset);
        } else {
            log.info("[KOSDAQ] disabled by config.");
        }

        // ===== 간단 검증 로그 =====
        long korCount = securityRepository.countByMarket(Market.KOR);
        log.info("[VERIFY] security KOR rows = {}", korCount);
    }

    private void importIfChanged(String location, Exchange exchange, Charset charset) throws Exception {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            log.warn("[{}] resource not found: {}", exchange, location);
            return;
        }

        String newsum = sha256Hex(resource);
        var recOpt = fileRepository.findByLocationAndExchange(location, exchange);

        if (recOpt.isPresent() && newsum.equals(recOpt.get().getChecksum())) {
            log.info("[{}] skip: checksum unchanged. ({})", exchange, location);
            return;
        }

        // 실제 적재
        KrxMasterLoader.LoadResult r;
        if (exchange == Exchange.KOSPI) r = loader.loadKospi(location, charset);
        else if (exchange == Exchange.KOSDAQ) r = loader.loadKosdaq(location, charset);

        // upsert 기록
        var record = recOpt.orElseGet(() -> KrxImportFile.builder()
                .location(location)
                .exchange(exchange)
                .checksum(newsum)
                .build());
        record.markImported(newsum, record.getProcessed(), record.getSuccess(), record.getSkipped(), record.getError());
        fileRepository.save(record);

        log.info("[{}] imported: processed={}, success={}, skipped={}, error={}, file={}",
                exchange, record.getProcessed(), record.getSuccess(), record.getSkipped(), record.getError(), record.getLocation());
    }

    private Charset toCharset(String name) {
        if (name == null || name.isBlank()) return StandardCharsets.UTF_8;
        var n = name.trim().toUpperCase();
        if (n.equals("CP949") || n.equals("MS949") || n.equals("EUC-KR")) return Charset.forName("MS949");
        if (n.equals("UTF8") || n.equals("UTF-8")) return StandardCharsets.UTF_8;
        return Charset.forName(name);
    }
}
