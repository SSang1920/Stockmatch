package com.stockmatch.corporate.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.corporate.analysis.Entity.AiAnalysisLog;
import com.stockmatch.corporate.analysis.dto.data.AnalysisPackage;
import com.stockmatch.corporate.analysis.dto.response.AiResponseDto;
import com.stockmatch.corporate.analysis.repository.AiAnalysisLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private RestTemplate restTemplate;
    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;
    private final AiAnalysisLogRepository logRepository;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model}")
    private String model;

    @PostConstruct
    public void init() {
        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

                    factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
                    factory.setReadTimeout((int) Duration.ofSeconds(20).toMillis());

                    return factory;
                }).build();
    }

    private static final String SYSTEM_PROMPT = """
            너는 투자 의사결정을 보조하는 분석가다.
            목표는 사용자가 현재 포트폴리오 관점에서 신규 기업이 어울리는 선택인지 이해하도록 돕는 것이다.
           
           [분석 규칙]:
           1. currentHoldings 정보는 포트폴리오의 '대략적인 투자 성향'을 추정하는 데만 사용한다.
           
           2. 보유 종목의 섹터, 재무 상태, 배당 여부, 밸류에이션을확정적으로 판단하거나 단정하지 마라.
            보유 종목에 대한 판단은 항상 "가능성이 있다", "보여진다"와 같은 완곡한 표현을 사용하라.
           
           3. 신규 기업(candidateCompany)에 대해서만 제공된 재무 지표와 데이터(businessPerformance, marketMomentum, financialHealth)를 근거로 특성과 위험도를 분석하라.
            제공되지 않은 정보는 추정하거나 만들어내지 마라.
           
           4. 최종 결론은 다음 관점에서 제시한다:"현재 포트폴리오 성향을 기준으로 볼 때,신규 기업이 이를 보완하는지,중립적인지,또는 부담을 키울 가능성이 있는지"
           
           5. 모든 판단은 추정임을 전제로 하며, 투자 조언처럼 보일 수 있는 과도한 확정 표현은 사용하지 마라.
           
           6. 설명은 투자 초보자도 이해할 수 있도록 어려운 금융 용어를 피하고, 일상적인 표현으로 부드럽게 풀어서 작성하라.
           판단의 이유(reasons)는 총 2~3개의 포인트로 정리하라
           각 근거는 "왜 이런 결론이 나왔는지" 직관적으로 알수 있게 1~2문장 내외로 간결하게 작성하라.
           """;

    public AiResponseDto getInvestmentAdvice(Long userId, String symbol, AnalysisPackage analysisPackage) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.2); // 창의성 억제

            // 데이터 전달
            String userDataJson = objectMapper.writeValueAsString(analysisPackage);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system","content", SYSTEM_PROMPT));
            messages.add(Map.of("role", "user", "content", userDataJson));
            requestBody.put("messages", messages);

            //출력 형식 강제
            requestBody.put("response_format", createJsonSchema());

            //헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<Map<String,Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("AI 분석 요청: Target={}", analysisPackage.getTargetStock().getName());
            JsonNode response = restTemplate.postForObject(apiUrl, entity, JsonNode.class);

            AiResponseDto resultDto = parseAiResponse(response);

            saveAnalysisLog(userId, symbol, resultDto);

            return resultDto;
        } catch (BusinessException e){
            log.warn("AI 분석 거절/ 실패 : Code = {} , Message = {}", e.getErrorCode(), e.getMessage());
            return createFallbackResponse();
        } catch (Exception e) {
            log.error("AI 분석 실패", e);
            return createFallbackResponse();
        }
    }

    // 출력 형식 통제
    private Map<String, Object> createJsonSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "json_schema");

        Map<String, Object> jsonSchema = new HashMap<>();
        jsonSchema.put("name", "investment_analysis");
        jsonSchema.put("strict", true); //

        Map<String, Object> schemaContent = new HashMap<>();
        schemaContent.put("type", "object");
        schemaContent.put("additionalProperties", false); // 정의되지 않은 필드 금지

        Map<String, Object> properties = new HashMap<>();

        properties.put("conclusionCode", Map.of(
                "type", "string",
                "enum", List.of("COMPLEMENTARY", "NEUTRAL" , "BURDEN"),
                "description" , "포트폴리오 적합성 코드"

        ));

        properties.put("oneLineReview", Map.of(
                "type", "string",
                "description" , "투자 성향에 따른 한 줄 요약"
        ));

        properties.put("reasons", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "description", "판단 근거 2~3가지 (각 항목은 1~2문장의 간결한 설명)"
        ));

        properties.put("disclaimer", Map.of(
                "type", "string",
                "description", "면책 조항"
        ));

        schemaContent.put("properties", properties);
        schemaContent.put("required", List.of("conclusionCode", "oneLineReview", "reasons" , "disclaimer"));

        jsonSchema.put("schema", schemaContent);
        schema.put("json_schema", jsonSchema);

        return schema;
    }

    private AiResponseDto parseAiResponse(JsonNode response) throws JsonProcessingException {
        if (response == null || !response.has("choices") || response.get("choices").isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_AI_RESPONSE);
        }

        JsonNode message = response.get("choices").get(0).get("message");

        // 거절 응답 체크
        if (message.has("refusal") && !message.get("refusal").isNull()) {
            throw new BusinessException(ErrorCode.AI_REFUSAL_RESPONSE);
        }

        return objectMapper.readValue(message.get("content").asText(), AiResponseDto.class);
    }

    private AiResponseDto createFallbackResponse() {
        return AiResponseDto.builder()
                .conclusionCode(AiResponseDto.ConclusionCode.NEUTRAL)
                .oneLineReview("현재 AI 분석 서비스 연결이 지연되고 있습니다.")
                .reasons(List.of("일시적인 네트워크 오류입니다.", "잠시 후 시도해주세요."))
                .disclaimer("시스템 오류로 인한 기본 응답입니다.")
                .build();
    }

    private void saveAnalysisLog(Long userId, String symbol, AiResponseDto dto){
        try{
            String jsonResult = objectMapper.writeValueAsString(dto);

            AiAnalysisLog logEntity = AiAnalysisLog.builder()
                    .userId(userId)
                    .symbol(symbol)
                    .aiResponseJson(jsonResult)
                    .build();

            logRepository.save(logEntity);

        } catch (Exception e){
            log.error("AI 분석 기록 저장중 오류 발생");
        }
    }

    public List<AiResponseDto> getUserHistory(Long userId) {
         List<AiAnalysisLog> logs = logRepository.findAllByUserIdOrderByAnalyzedAtDesc(userId);

         return logs.stream()
                 .map(logEntity -> {
                     try {
                         return objectMapper.readValue(logEntity.getAiResponseJson(), AiResponseDto.class);
                     } catch (Exception e) {
                         log.error("히스토리 파싱 실패");
                         return null;
                     }
                 })
                 .filter(Objects::nonNull)
                 .toList();
    }
}
