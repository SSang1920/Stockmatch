package com.stockmatch.stock.importer;

import com.stockmatch.stock.domain.Exchange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsMasterLoader {

    private final UsMasterService usMasterService;
    private final ResourceLoader resourceLoader;

    public record LoadResult(int processed, int success, int skipped, int error) { }

    /**
     * NASDAQ CSV 적재
     */
    public LoadResult loadNasdaq(String location, Charset charset) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalArgumentException("파일 없음: " + location);
        }

        return loadCsvGeneric(resource, charset, Exchange.NASDAQ);
    }

    /**
     * NYSE CSV 적재
     */
    public LoadResult loadNyse(String location, Charset charset) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalArgumentException("파일 없음: " + location);
        }

        return loadCsvGeneric(resource, charset, Exchange.NYSE);
    }

    private LoadResult loadCsvGeneric(Resource resource, Charset charset, Exchange exchange) throws IOException {
        int processed = 0, success = 0, skipped = 0, error = 0;

        try (var in = resource.getInputStream();
             var reader = new InputStreamReader(in, charset)) {

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();

            try (CSVParser parser = new CSVParser(reader, format)) {
                // 헤더 인덱스
                List<String> header = parser.getHeaderMap().keySet().stream()
                        .map(String::trim)
                        .toList();

                int idxTicker = indexOf(header, "Symbol");
                int idxName = indexOf(header, "Korea name");
                int idxSecType = indexOf(header, "Security type(1:Index,2:Stock,3:ETP(ETF),4:Warrant)");

                if (idxTicker < 0 || idxName < 0) {
                    throw new IllegalArgumentException("필수 헤더 누락: [Symbol], [Name]");
                }

                for (CSVRecord record : parser) {
                    processed++;

                    String ticker = record.get(idxTicker).trim();
                    String name = record.get(idxName).trim();
                    String secType = (idxSecType >= 0) ? record.get(idxSecType).trim() : null;

                    if (ticker.isEmpty()) {
                        skipped++;
                        continue;
                    }

                    try {
                        if (exchange == Exchange.NASDAQ) {
                            usMasterService.upsertUsNasdaq(ticker, name, secType);
                        } else if (exchange == Exchange.NYSE) {
                            usMasterService.upsertUsNyse(ticker, name, secType);
                        }

                        success++;
                    } catch (Exception e) {
                        error++;
                        if (error <= 10) {
                            log.warn("[row={}] upsert 실패: ticker={}, name={}, err={}",
                                    processed, ticker, name, e.toString());
                        }
                    }
                }
            }
        }

        log.info("US {} 적재 완료: processed={}, success={}, skipped={}, error={}, file={}",
                exchange, processed, success, skipped, error, resource.getDescription());

        return new LoadResult(processed, success, skipped, error);
    }

    private static int indexOf(List<String> header, String key) {
        for (int i = 0; i < header.size(); i++) {
            if (key.equals(header.get(i))) return i;
        }
        return -1;
    }
}
