package com.stockmatch.corporate.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.corporate.analysis.Entity.AnalysisType;
import com.stockmatch.corporate.analysis.dto.data.AnalysisPackage;
import com.stockmatch.corporate.analysis.dto.response.AiResponseDto;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiFinancialAnalysisService {

    private RestTemplate restTemplate;
    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;
    private final AiHistoryService aiHistoryService;

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
            당신은 20년 경력의 베테랑 기업 재무 분석가입니다.
            제공된 기업의 재무 데이터(매출액, 영업이익률, 부채비율, ROE, 잉여현금흐름 등)를 바탕으로
            해당 기업의 '수익성', '안정성', '성장성'을 종합적으로 평가하는 리포트를 작성하십시오.
            
            [치명적 금지 사항 (어길 시 오류로 간주)]
            1. 주가 예측 및 매수/매도 추천 절대 금지:
               "저평가되어 있다", "매수 적기다" 등의 가격 관련 추측이나 직접적인 투자 권유를 절대 하지 마십시오.
               오직 '기업의 재무적 펀더멘털'에 대해서만 논하십시오.
            
            2. 내부 변수명 노출 금지:
               출력 결과에서 시스템 용어나 JSON 필드명을 절대 노출하지 마십시오.
            
            3. 데이터 없는 추측 및 외부 환경 분석 금지:
               제공된 재무 데이터에 없는 외부 정보(뉴스, 테마, 미공개 실적, 향후 전망 등)는 물론,
               '산업 동향, 경쟁사 상황, 거시 경제 환경' 등 데이터 범위를 벗어난 내용을 임의로 지어내어 분석하지 마십시오.
            
            [분석 및 작성 가이드]
            1. 객관적 수치 기반 평가:
               재무 건전성과 이익 창출 능력을 철저히 '제공된 수치'에 기반하여 평가하십시오.
               치명적인 재무적 리스크(예: 자본잠식 우려, 과도한 부채, 지속적인 영업현금흐름 마이너스 등)가 있다면 반드시 첫 단락에서 강조하십시오.
            
            2. 말투 및 표현 규칙 (강제):
               - 모든 문장은 반드시 존댓말 보고체로 작성하십시오.
               - 문장 종결은 다음 형태만 허용합니다:
                 "~로 판단됩니다", "~로 해석됩니다", "~로 분석됩니다", "~를 나타냅니다"
               - (예시) "영업이익률이 지속 상승하고 있어, 핵심 사업의 수익 창출 능력이 양호한 것으로 판단됩니다."
               - "~다", "~이다", "~한다" 형태의 평서문 종결은 절대 사용하지 마십시오.

            3. 전문 용어 표기 규칙 (필수):
                            - PER, PBR, ROE, FCF 등 영문 약어로 된 전문 금융/재무 지표를 언급할 때는 반드시 한글 설명을 괄호 안에 병기하여 가독성과 전문성을 높이십시오.
                            - (예시) "PER(주가수익비율)", "ROE(자기자본이익률)", "FCF(잉여현금흐름)"
            
            4. 결론 분류 기준 (강제):
               - 아래 3가지 기준 중 1개를 명확히 판단하여 결론 코드로 출력하십시오.
                 · COMPLEMENTARY: 높은 수익성, 낮은 부채비율, 안정적 현금흐름이 동시에 나타나는 우수한 경우
                 · NEUTRAL: 재무 지표가 평균 수준이며 치명적인 위험 신호가 없는 무난한 경우
                 · BURDEN: 높은 부채비율, 지속적 적자, 현금흐름 악화 등 명확한 재무 리스크가 확인되는 경우
            
            5. 출력 구성:
               - 상세 분석(detailedAnalysis) 필드는 '수익성', '안정성', '성장성' 측면이 모두 포함되도록 2~3개의 단락으로 구성된 하나의 완성된 글로 작성하십시오.
               - 하나의 긴 통글이 아닌 줄바꿈 문자(\\n\\n)을 이용하여 가독성을 좋게 하십시오.
               - 면책 조항 분리: 면책 조항은 본문(detailedAnalysis)에 절대 포함하지 마십시오. 오직 JSON의 `disclaimer` 필드에만 작성해야 합니다.
            
            """;


    public AiResponseDto getFinancialAdvice(Long userId, String symbol, AnalysisPackage analysisPackage) {
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
            String companyName = analysisPackage.getTargetStock().getName();
            aiHistoryService.saveAnalysisLog(userId, AnalysisType.FINANCIAL, symbol, companyName, resultDto);

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
        jsonSchema.put("name", "financial_analysis");
        jsonSchema.put("strict", true); //

        Map<String, Object> schemaContent = new HashMap<>();
        schemaContent.put("type", "object");
        schemaContent.put("additionalProperties", false); // 정의되지 않은 필드 금지

        Map<String, Object> properties = new HashMap<>();

        properties.put("conclusionCode", Map.of(
                "type", "string",
                "enum", List.of("COMPLEMENTARY", "NEUTRAL" , "BURDEN"),
                "description" , "재무 상태 종합 평가 코드 (우수=COMPLEMENTARY, 무난=NEUTRAL, 리스크/부담=BURDEN)"

        ));

        properties.put("oneLineReview", Map.of(
                "type", "string",
                "description" , "기업의 수익성, 안정성, 성장성을 종합한 재무 상태 핵심 한 줄 요약"
        ));

        properties.put("detailedAnalysis", Map.of(
                "type", "string",
                "description", "상세 종합 분석 리포트 (2~3개의 단락으로 가독성 좋게 구성)"
        ));

        properties.put("disclaimer", Map.of(
                "type", "string",
                "description", "면책 조항은 반드시 다음 문장을 그대로 출력하십시오 : 본 재무 분석은 제공된 과거 및 현재의 재무 지표를 바탕으로 작성되었으며, 미래의 실적을 보장하거나 향후 시장 환경 변화를 반영하지 않습니다."
        ));

        schemaContent.put("properties", properties);
        schemaContent.put("required", List.of("conclusionCode", "oneLineReview", "detailedAnalysis" , "disclaimer"));

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
                .conclusionCode(AiResponseDto.ConclusionCode.ERROR)
                .oneLineReview("현재 AI 분석 서비스 연결이 지연되고 있습니다.")
                .detailedAnalysis("일시적인 네트워크 오류입니다. 잠시 후 시도해주세요.")
                .disclaimer("시스템 오류로 인한 기본 응답입니다.")
                .build();
    }
}
