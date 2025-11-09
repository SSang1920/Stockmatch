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
import java.nio.charset.Charset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KrxMasterLoader {

    private final KrxMasterService krxMasterService;
    private final ResourceLoader resourceLoader;

    public record LoadResult(int processed, int success, int skipped, int error) { }

    /**
     * KOSPI CSV 적재
     */
    public LoadResult loadKospi(String location, Charset charset) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalArgumentException("파일 없음: " + location);
        }

        return loadCsvGeneric(resource, charset, Exchange.KOSPI);
    }

    /**
     * KOSDAQ CSV 적재
     */
    public LoadResult loadKosdaq(String location, Charset charset) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalArgumentException("파일 없음: " + location);
        }

        return loadCsvGeneric(resource, charset, Exchange.KOSDAQ);
    }

    private LoadResult loadCsvGeneric(Resource resource, Charset charset, Exchange exchange) throws IOException {
        int processed = 0, success = 0, skipped = 0, error = 0;

        try (var in = resource.getInputStream();
             var reader = new java.io.InputStreamReader(in, charset)) {

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();

            try (CSVParser parser = new CSVParser(reader, format)) {
                // 헤더 인덱스
                var header = parser.getHeaderMap().keySet().stream().map(String::trim).toList();
                var idxTicker = indexOf(header, "단축코드");
                int idxIsin = indexOf(header, "표준코드");
                int idxName = firstPresent(
                        indexOf(header, "한글명"),
                        indexOf(header, "한글종목명")
                );
                if (idxTicker < 0 || idxIsin < 0 || idxName < 0) {
                    throw new IllegalArgumentException("필수 헤더 누락: [단축코드], [표준코드], [한글명|한글종목명]");
                }

                for (CSVRecord record : parser) {
                    processed++;
                    String ticker = record.get(idxTicker).trim();
                    String isin = record.get(idxIsin).trim();
                    String name = record.get(idxName).trim();

                    if (ticker.isEmpty()) {
                        skipped++;
                        continue;
                    }

                    try {
                        if (exchange == Exchange.KOSPI) krxMasterService.upsertKrxKospi(ticker, isin, name);
                        else if (exchange == Exchange.KOSDAQ) krxMasterService.upsertKrxKosdaq(ticker, isin, name);
                        success++;
                    } catch (Exception e) {
                        error++;
                        if (error <= 10) {
                            log.warn("[row={}] upsert 실패: ticker={}, isin={}, name={}, err={}",
                                    processed, ticker, isin, name, e.toString());
                        }
                    }
                }
            }
        }

        log.info("KRX {} 적재 완료: processed={}, success={}, skipped={}, error={}, file={}",
                exchange, processed, success, skipped, error, resource.getDescription());

        return new LoadResult(processed, success, skipped, error);
    }

    private static int indexOf(List<String> header, String key) {
        for (int i = 0; i < header.size(); i++) {
            if (key.equals(header.get(i))) return i;
        }
        return -1;
    }

    private static int firstPresent(int... candidates) {
        for (int c : candidates) if (c >= 0) return c;
        return -1;
    }
}
