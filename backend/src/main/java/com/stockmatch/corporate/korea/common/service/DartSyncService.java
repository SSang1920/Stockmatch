package com.stockmatch.corporate.korea.common.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.corporate.korea.common.infra.DartCorpCodeClient;
import com.stockmatch.corporate.korea.common.infra.GenericDartClient;
import com.stockmatch.corporate.korea.common.domain.DartCorpCode;
import com.stockmatch.corporate.korea.common.domain.DartCorpCodeRepository;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DartSyncService {

    private final DartCorpCodeClient dartCorpCodeClient;
    private final DartCorpCodeRepository dartCorpCodeRepository;
    private final SecurityRepository securityRepository;

    @Transactional
    public void syncCorpCodes() {
        try{
            byte[] zipData = dartCorpCodeClient.getCorpCodeList();

            if (zipData == null || zipData.length == 0) {
                throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
            }

            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    throw new BusinessException(ErrorCode.DART_FILE_ERROR);
                }

                // XML 내용 읽기
                String xmlContent = new String(zis.readAllBytes(), StandardCharsets.UTF_8);

                // XML 파싱 및 저장
                parseAndSaveXml(xmlContent);
            }
        } catch (Exception e){
            log.error("DART 고유 번호 동기화 중 오류 발생" ,e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

    }
    private void parseAndSaveXml (String xmlContent) {
        log.info("DART XML 데이터 파싱 및 저장 시작");

        XMLInputFactory factory = XMLInputFactory.newInstance();
        List<DartCorpCode> batchList = new ArrayList<>();
        int count = 0;

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xmlContent));
            String currentCorpCode = null;
            String currentStockCode = null;
            String currentTagName = "";

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT -> currentTagName = reader.getLocalName();
                    case XMLStreamConstants.CHARACTERS -> {
                        String text = reader.getText().trim();
                        if (text.isEmpty()) break;
                        if ("corp_code".equals(currentTagName)) currentCorpCode = text;
                        if ("stock_code".equals(currentTagName)) currentStockCode = text;
                    }
                    case XMLStreamConstants.END_ELEMENT -> {
                        if ("list".equals(reader.getLocalName())) {
                            if (currentStockCode != null && !currentStockCode.isEmpty()) {
                                processMapping(currentStockCode, currentCorpCode, batchList);
                                count++;
                            }
                            currentCorpCode = null;
                            currentStockCode = null;
                        }
                        currentTagName ="";
                    }
                }

                //1000건 마다 로그 출력 및 Batch 저장
                if(batchList.size() >= 1000) {
                    dartCorpCodeRepository.saveAll(batchList);
                    batchList.clear();
                    log.info("{} 건 처리중...", count);
                }
            }

            if (!batchList.isEmpty()){
                dartCorpCodeRepository.saveAll(batchList);
            }
            log.info("총 {} 개의 상장사 고유번호 저장 완료", count);

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DART_SYNC_ERROR);
        }
    }

    private void processMapping(String stockCode, String corpCode, List<DartCorpCode> batchList) {
        securityRepository.findByTicker(stockCode).ifPresent(security ->{
            batchList.add(DartCorpCode.builder()
                    .ticker(stockCode)
                    .corpCode(corpCode)
                    .security(security)
                    .build());
        });
    }

}
