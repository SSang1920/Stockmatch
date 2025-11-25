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
@ConditionalOnProperty(prefix = "import.us", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class UsMasterImportRunner implements CommandLineRunner {

    private final UsMasterLoader loader;
    private final SecurityRepository securityRepository;
    private final ImportFileRepository fileRepository;
    private final ResourceLoader resourceLoader;

    @Value("${import.us.nasdaq-location:classpath:import/nasdaq_listed.csv}")
    private String nasdaqLocation;

    @Value("${import.us.nyse-location:classpath:import/nyse_listed.csv}")
    private String nyseLocation;

    @Value("${import.us.load-nasdaq:true}")
    private boolean loadNasdaq;

    @Value("${import.us.load-nyse:true}")
    private boolean loadNyse;

    @Value("${import.us.encoding:UTF-8}")
    private String encoding;

    @Override
    public void run(String... args) throws Exception {
        Charset charset = toCharset(encoding);

        if (loadNasdaq) {
            importIfChanged(nasdaqLocation, Exchange.NASDAQ, charset);
        } else {
            log.info("[NASDAQ] disabled by config.");
        }

        if (loadNyse) {
            importIfChanged(nyseLocation, Exchange.NYSE, charset);
        } else {
            log.info("[NYSE] disabled by config.");
        }

        // 간단 검증 로그
        long usCount = securityRepository.countByMarket(Market.US);
        log.info("[VERIFY] security US rows = {}", usCount);
    }

    /**
     * 파일 checksum이 변경되었을 때만 import 수행
     */
    private void importIfChanged(String location, Exchange exchange, Charset charset) throws Exception {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            log.warn("[{}] resource not found: {}", exchange, location);
            return;
        }

        // 새 파일 기준 checksum 계산
        String newsum = sha256Hex(resource);

        // 기존 기록 조회
        var recOpt = fileRepository.findByLocationAndExchange(location, exchange);

        // checksum 동일하면 스킵
        if (recOpt.isPresent() && newsum.equals(recOpt.get().getChecksum())) {
            log.info("[{}] skip: checksum unchanged. ({})", exchange, location);
            return;
        }

        // 실제 CSV 적재
        UsMasterLoader.LoadResult r;
        if (exchange == Exchange.NASDAQ) r = loader.loadNasdaq(location, charset);
        else if (exchange == Exchange.NYSE) r = loader.loadNyse(location, charset);

        // upsert 결과 기록
        var record = recOpt.orElseGet(() -> ImportFile.builder()
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
