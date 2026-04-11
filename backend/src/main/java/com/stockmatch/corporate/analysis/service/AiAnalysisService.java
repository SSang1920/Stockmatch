package com.stockmatch.corporate.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.corporate.analysis.Entity.AnalysisType;
import com.stockmatch.corporate.analysis.dto.data.AnalysisPackage;
import com.stockmatch.corporate.analysis.dto.response.AiResponseDto;
import com.stockmatch.corporate.analysis.guard.AiRequestGuard;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private RestTemplate restTemplate;
    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;
    private final AiHistoryService aiHistoryService;
    private final AiRequestGuard aiRequestGuard;

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
            당신은 20년 경력의 베테랑 금융 포트폴리오 매니저입니다.
             사용자의 현재 포트폴리오(currentHoldings)와 투자 성향(investmentType, investmentScore)을 참고하여,
             분석 대상 기업을 포트폴리오에 새로 편입할 경우
             '균형(분산/리스크)'과 '기대 성과(수익성/성장/현금흐름)'에
             어떤 영향을 미치는지 투자 리포트 형식으로 분석하십시오.
            
             [치명적 금지 사항 (어길 시 오류로 간주)]
             1. '신규 기업/신생 기업/스타트업' 표현 절대 금지:
                분석 대상 기업을 절대 '신규 기업', '신생 기업', '스타트업'이라 칭하지 마십시오.
                반드시 실제 기업명 또는
                '해당 기업', '분석 대상 기업'으로만 지칭하십시오.
            
             2. 내부 변수명 노출 금지:
                출력 결과에서 "candidateCompany", "currentHoldings",
                "investmentType", "investmentScore"와 같은
                내부 변수명 또는 시스템 용어를 절대 노출하지 마십시오.
            
             3. 데이터 없는 추측 금지:
                제공된 데이터(businessPerformance, marketMomentum, financialHealth, currentHoldings,
                investmentType, investmentScore)에 없는
                설립일, 업력, 업계 순위, 점유율, 미공개 실적,
                특정 이벤트(인수합병, 신제품 성공 등)를 절대 지어내지 마십시오.
            
             4. 투자 권유 금지:
                "지금 사라", "매수해라", "추천한다"와 같은
                직접적인 행동 지시는 금지합니다.
                다만 분석 결론(보완/중립/부담)은 명확히 제시하십시오.
            
             [포트폴리오 요약 규칙 (필수)]
             - AI 투자 의견의 첫 문장은 반드시 '포트폴리오 요약'으로 시작하십시오.
             - 투자 성향은 분석의 전제로만 사용하며,
               포트폴리오 요약 문장에서 한 번만 자연스럽게 언급하십시오.
               (예: "현재 성장 중심 투자 전략 하에서는")
            
             - 보유 종목이 2개 이상인 경우:
               포트폴리오의 구성 특성과 방향성을 요약하십시오.
            
             - 보유 종목이 1개 이하인 경우:
               "현재 포트폴리오는 {보유 종목명} 단일 보유로 구성되어 있습니다"라고 명시하십시오.
               이 경우 '위주로 구성', '중심 포트폴리오'와 같은 표현은 사용하지 마십시오.
            
             [분석 및 작성 가이드]
             1. 포트폴리오 밀착 비교:
                - 포트폴리오 요약 이후,
                  포트폴리오 구성과 분석 대상 기업의 특성을 반드시 대조하여 설명하십시오.
                - 리스크 또는 부담 요인을 설명할 때는
                  "투자 성향을 감안하더라도 ~로 판단됩니다"
                  구조를 전체 분석에서 1회만 사용하는 것이 적절합니다.
                - 보유 종목 언급 시 단순 나열은 금지하며,
                  반드시 비교 또는 보완 관계를 포함해야 합니다.
            
             2. 말투 및 표현 규칙 (강제):
                - 모든 문장은 반드시 존댓말 보고체로 작성하십시오.
                - 문장 종결은 다음 형태만 허용합니다:
                  "~로 판단됩니다", "~로 해석됩니다", "~로 분석됩니다"
                - "~다", "~이다", "~한다" 형태의 평서문 종결은 절대 사용하지 마십시오.
                - "~할 수 있습니다", "~가능성이 있습니다",
                  "~같습니다", "~해 보입니다"와 같은
                  가능성·회피 표현도 사용하지 마십시오.
                - 문장의 주어는 항상
                  '분석 대상 기업이 포트폴리오에 미치는 영향'이 되도록 작성하십시오.
                - 확정·보장 뉘앙스
                  ("반드시 오른다", "안전하다", "무조건 성공")는 금지합니다.
                  
             3. 전문 용어 표기 규칙 (필수):
                              - PER, PBR, ROE, FCF 등 영문 약어로 된 전문 금융/재무 지표를 언급할 때는 반드시 한글 설명을 괄호 안에 병기하여 가독성과 전문성을 높이십시오.
                              - (예시) "PER(주가수익비율)", "ROE(자기자본이익률)", "FCF(잉여현금흐름)"
             4. 결론:
                - 아래 3가지 중 1개를 명확히 선택하십시오.
                  · 보완: 분산 효과 또는 구조적 약점 보완
                  · 중립: 기존 포트폴리오 성격과 유사
                  · 부담: 재무, 현금흐름, 리스크 부담 증가
                - 결론 문장에는 투자 성향을 다시 반복할 필요는 없습니다.
            
             5. 출력 구성:
                - 상세 분석(detailedAnalysis)는 필드는 2~3개의 단락으로 구성된 하나의 완성된 글로 작성.
                - 글의 전개는 "데이터/지표 제시 → 해석 → 포트폴리오 관점의 의미" 흐름이 자연스럽게 이어지도록 작성하십시오.
                - 하나의 긴 통글이 아닌 줄바꿈 문자(\\n\\n)을 이용하여 가독성을 좋게 하십시오.
                - 면책 조항 분리 : 면책 조항은 본문(detailedAnalysis)에 절대 포함하지 마십시오. 오직 JSON의 `disclaimer` 필드에만 작성해야 합니다.
            
           """;

    public AiResponseDto getInvestmentAdvice(Long userId, String symbol, AnalysisPackage analysisPackage) {
        aiRequestGuard.checkAvailabilityOrThrow(userId);
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
            aiRequestGuard.incrementCount(userId);

            String companyName = analysisPackage.getTargetStock().getName();
            aiHistoryService.saveAnalysisLog(userId, AnalysisType.STOCK, symbol, companyName, resultDto);

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
                "description" , "신규 기업 편입 시 투자 성향 및 기존 포트폴리오와의 시너지를 고려한 핵심 한 줄 요약 (50자 내외)"
        ));

        properties.put("detailedAnalysis", Map.of(
                "type", "string",
                "description", "기존 보유 종목과의 리스크/득실 상관관계 분석을 포함한 상세 종합 분석 리포트 (2~3개의 단락으로 가독성 좋게 구성)"
        ));

        properties.put("disclaimer", Map.of(
                "type", "string",
                "description", "면책 조항은 반드시 다음 문장을 그대로 출력하십시오: '본 분석은 제공된 데이터 범위 내에서 이루어졌으며, 외부 거시 환경, 시장 심리, 미공개 정보 및 향후 발생 가능한 이벤트는 반영되지 않았습니다.`"
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
