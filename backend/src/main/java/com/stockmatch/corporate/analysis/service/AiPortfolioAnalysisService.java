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
public class AiPortfolioAnalysisService {

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
            당신은 20년 경력의 베테랑 전략가이자 자산 관리자(Portfolio Manager)입니다.
            제공된 데이터를 바탕으로 포트폴리오를 심층 진단하고, 날카로운 전문가적 통찰을 제공하십시오.

            [1. 치명적 금지 사항]
            - 데이터 해석: 비중(weightPct)을 최우선 기준으로 판단하십시오. amount(금액) 수치는 통화 단위가 다를 수 있으므로 절대적인 크기 비교 기준으로 사용하지 마십시오.
            - 재계산 금지: 제공된 비중 수치를 임의로 합산하거나 변형하지 마십시오.
            - 종목 추천 금지: 특정 종목의 매수/매도 권유를 절대 하지 마십시오. 자산 배분 관점의 '섹터'만 논하십시오.
            - 내부 변수명 노출 금지 및 JSON 형식 엄수.

            [2. 분석 및 진단 원칙]
            - 성향 일치도 평가: 투자 성향(investmentType)과 현재 포트폴리오의 리스크 수준이 일치하는지 반드시 평가하고, 불일치 시 그 위험성을 강력히 경고하십시오.
            - 주력 섹터 정의: 비중(weightPct)이 가장 높은 종목 또는 해당 종목이 속한 섹터를 '주력 섹터'로 정의하고 분석의 기점으로 삼으십시오.
            - 맞춤형 연결 분석 (필수): 제안하는 자산군을 설명할 때 일반적인 특징(예: 채권은 안전하다)을 나열하지 마십시오. 반드시 '주력 섹터의 약점'과 연결하여 설명하십시오. (예: "반도체의 높은 경기 민감도를 상쇄하기 위해 역상관관계인 채권을 제안합니다")
            - 전략적 다각화: 매번 다른 관점(거시경제, 금리 환경, 경기 사이클 등)을 최소 1개 이상 반영하여 기계적인 반복 답변을 지양하십시오.

            [3. 말투 및 표현 규칙]
            - 반드시 전문가용 존댓말 보고체로 작성하십시오. (~로 판단됩니다, ~를 제안해 드립니다 등)
            - 금지 어미: "~다", "~이다", "~한다", "~입니다", "~습니다" (절대 사용 금지)

            [4. 결론 코드 분류 기준]
            - WELL_BALANCED: 단일 종목 비중 60% 미만이며 투자 성향과 부합하는 경우
            - CONCENTRATED: 단일 종목 비중 60% 이상 80% 미만인 경우
            - HIGH_RISK: 단일 종목 비중 80% 이상이거나, 투자 성향 대비 리스크가 극심한 경우

            [5. 출력 구성 및 전개 방식]
            - [oneLineReview] 질문에 대한 핵심 답변 또는 리스크 한 줄 요약 (50자 내외).
            - [detailedAnalysis] 반드시 2개의 단락으로 구성하고 단락 사이는 \\\\n\\\\n으로 구분하십시오. 각 단락은 최소 3문장 이상의 구체적 서술을 포함해야 합니다.

            - (사용자 질문이 있는 경우)
              · 1단락: 질문 답변 및 주력 섹터와 문의 자산 간의 상관관계/리스크 분석.
              · 2단락: (필수) 부족한 부분을 보완할 수 있는 구체적인 추천 섹터/자산군 2~3개 제안 및 선정 이유 명시.

            - (사용자 질문이 없는 경우)
              · 1단락: 비중 수치를 근거로 한 주력 섹터 쏠림 진단 및 투자 성향 부합 여부 서술.
              · 2단락: (필수) 리스크 상쇄를 위한 구체적인 추천 섹터/자산군 2~3개 제안 및 보완 효과 서술.

            [6. 답변 예시]
            - 사용자 질문: "어떤 섹터를 추가하면 좋을까?"
            - 허용 답변: "현재 반도체에 90% 이상 쏠린 포트폴리오는 경기 하강 국면에서 취약할 수 있습니다. 이를 보완하기 위해 금리 인하 수혜가 예상되는 리츠(REITs)나 배당 안정성이 높은 유틸리티 섹터 편입을 제안해 드립니다."
            ""\";
           """;

    public AiResponseDto getPortfolioAdvice(Long userId, AnalysisPackage analysisPackage , String userComment) {
        aiRequestGuard.checkAvailabilityOrThrow(userId);
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.2); // 창의성 억제

            // 프롬프트 전달
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

            String safeUserComment = (userComment == null || userComment.trim().isEmpty())
                    ? "현재 포트폴리오 상태에 대한 종합적인 진단을 부탁드립니다."
                    : userComment;

            // 데이터 전달 (포트폴리오 + 유저 코멘트)
            String userDataJson = objectMapper.writeValueAsString(analysisPackage);
            String combinedContent = String.format(
                    "사용자 질문: %s\n\n분석할 포트폴리오 데이터: %s",
                    safeUserComment,
                    userDataJson
            );
            messages.add(Map.of("role", "user", "content", combinedContent));
            requestBody.put("messages", messages);

            //출력 형식 강제
            requestBody.put("response_format", createJsonSchema());

            //헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<Map<String,Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("AI 포트폴리오 분석 요청: UserId={}", userId);
            JsonNode response = restTemplate.postForObject(apiUrl, entity, JsonNode.class);

            AiResponseDto responseDto = parseAiResponse(response);
            aiRequestGuard.incrementCount(userId);

            AiResponseDto resultDto = responseDto.toBuilder()
                    .currentHoldings(analysisPackage.getUser().getCurrentHoldings())
                    .build();

            aiHistoryService.saveAnalysisLog(userId, AnalysisType.PORTFOLIO, null ,null ,userComment , resultDto);

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
        jsonSchema.put("name", "portfolio_analysis");
        jsonSchema.put("strict", true); //

        Map<String, Object> schemaContent = new HashMap<>();
        schemaContent.put("type", "object");
        schemaContent.put("additionalProperties", false); // 정의되지 않은 필드 금지

        Map<String, Object> properties = new HashMap<>();

        properties.put("conclusionCode", Map.of(
                "type", "string",
                "enum", List.of("WELL_BALANCED", "CONCENTRATED" , "HIGH_RISK"),
                "description" , "포트폴리오 적합성 코드"

        ));

        properties.put("oneLineReview", Map.of(
                "type", "string",
                "description" , "사용자 질문에 대한 핵심 답변 또는 현재 포트폴리오 상태에 대한 명확한 한 줄 요약(50자 내외)"
        ));

        properties.put("detailedAnalysis", Map.of(
                "type", "string",
                "description", "질문에 대한 상세 답변 및 포트폴리오 종합 진단 리포트 (비중 쏠림, 개선점 포함)"
        ));

        properties.put("disclaimer", Map.of(
                "type", "string",
                "description", "면책 조항은 반드시 다음 문장을 그대로 출력하십시오: 본 진단은 현재 보유하신 종목의 비중을 바탕으로 한 포트폴리오 관점의 분석이며, 개별 종목의 미래 수익률을 보장하지 않습니다"
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
