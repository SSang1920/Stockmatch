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

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPortfolioAnalysisService {

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
            당신은 20년 경력의 베테랑 자산 관리자(Portfolio Manager)입니다.
            사용자의 '투자 성향'과 '현재 보유 중인 포트폴리오 전체 데이터(종목, 비중 등)'를 바탕으로 포트폴리오의 건강 상태를 진단하십시오.

            [치명적 금지 사항 (어길 시 오류로 간주)]
            1. 계산 절대 금지:\s
               스스로 금액이나 비중을 절대 계산하거나 더하지 마십시오. 오직 제공된 '비중(%)' 수치 자체만 보고 판단하십시오.
            2. 특정 종목 매수/매도 및 가격 예측 절대 금지:\s
               "~를 추가 매수하라", "~를 팔아라" 등의 직접적인 투자 권유를 절대 하지 마십시오. 전체적인 '비중'과 '리스크 밸런스' 관점만 논하십시오.
            3. 내부 변수명 노출 금지:\s
               출력 결과에서 시스템 용어나 JSON 필드명을 절대 노출하지 마십시오.

            [분석 및 작성 가이드]
            1. 비중 및 쏠림 현상 진단 (분석 핵심):
               - 제공된 비중 데이터를 바탕으로, 특정 섹터(예: 기술주)나 단일 종목에 자산이 과도하게 쏠려 있는지(Concentration Risk) 진단하십시오.
               - 사용자의 투자 성향(investmentType)과 현재 포트폴리오의 구성(예: 안정형 성향인데 기술주 비중이 90% 이상인 경우 등)이 부합하는지 평가하십시오.

            2. 말투 및 표현 규칙 (강제):
               - 모든 문장은 단락의 중간과 끝을 불문하고 반드시 존댓말 보고체로 작성하십시오.
               - 허용된 문장 종결 형태: "~로 판단됩니다", "~로 분석됩니다", "~로 해석됩니다", "~를 나타냅니다"
               - 금지된 문장 종결 형태: "~다", "~이다", "~한다", "~입니다", "~습니다" (절대 사용 금지)
               - (예시) "특정 종목의 비중이 70%를 초과하여 리스크 관리가 필요한 것으로 분석됩니다."

            3. 결론 분류 기준 (강제):
               - 아래 3가지 중 1개를 명확히 판단하여 결론 코드로 출력하십시오.
                 · WELL_BALANCED: 단일 종목/섹터에 과도한 쏠림이 없고, 사용자의 투자 성향과 잘 부합하는 경우
                 · CONCENTRATED: 특정 종목이나 단일 섹터의 비중이 비정상적으로 높아(예: 60% 이상) 분산이 필요한 경우
                 · HIGH_RISK: 사용자의 투자 성향(예: 안정형) 대비 변동성이 큰 자산(예: 기술주 몰빵)의 비중이 너무 커서 즉각적인 위험 관리가 필요한 경우

            4. 출력 구성 및 사용자 질문 반영 (강제):
                입력된 데이터에 '사용자 질문(userComment)'이 존재하는지 여부에 따라 아래 규칙을 엄격히 따르십시오.

                - [oneLineReview (한 줄 요약)]
                  · 질문이 있는 경우: 질문에 대한 핵심 결론만 50자 내외로 짧고 명확하게 제시하십시오.
                  · 질문이 없는 경우: 현재 포트폴리오의 가장 큰 특징이나 리스크에 대한 한 줄 요약을 작성하십시오.

                - [detailedAnalysis (상세 분석 리포트)]
                  · 2~3개의 단락으로 구성하며, 반드시 단락 사이에 줄바꿈 문자(\\\\n\\\\n)를 사용하여 가독성을 높이십시오.
                  · 면책 조항 분리 (강제): 본문에는 면책 조항을 절대 포함하지 마십시오. 오직 JSON의 `disclaimer` 필드에만 작성해야 합니다.

                  [질문이 있는 경우의 전개 방식]
                  · 1단락: 한 줄 요약의 문장을 그대로 반복하지 마십시오. 대신, 한 줄 요약 결론의 '구체적인 이유'를 서술하십시오. 이때 '기존 보유 종목'과 '사용자가 문의한 자산' 간의 상관관계, 리스크 분산 효과, 득실을 전문가적 시선에서 심도 있게 대조 분석하십시오.
                  · 2단락: 현재 포트폴리오 전체의 비중 리스크 진단 및 궁극적인 개선 방향을 자연스럽게 이어가십시오.

                  [질문이 없는 경우의 전개 방식]
                  · 현재 비중 상태 요약 -> 투자 성향 일치도 -> 포트폴리오 개선 방향 순서로 서술하십시오.

                (답변 예시)
                - 사용자 질문: "어떤 섹터를 추가하면 좋을까?"
                - 허용된 답변: "단일 종목 집중 포트폴리오의 경우 기술, 헬스케어, 소비재 등 서로 다른 산업 구조를 가진 섹터를 포함하여 분산하는 방향이 리스크 관리 측면에서 도움이 될 수 있는 것으로 분석됩니다."
                - 금지된 답변: "엔비디아를 매수하는 것이 좋습니다."
           """;

    public AiResponseDto getPortfolioAdvice(Long userId, AnalysisPackage analysisPackage , String userComment) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.2); // 창의성 억제

            // 프롬프트 전달
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

            // 데이터 전달 (포트폴리오 + 유저 코멘트)
            String userDataJson = objectMapper.writeValueAsString(analysisPackage);
            String combinedContent = String.format(
                    "사용자 질문: %s\n\n분석할 포트폴리오 데이터: %s",
                    userComment,
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
