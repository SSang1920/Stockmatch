package com.stockmatch.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.config.jwt.JwtAuthenticationFilter;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.portfolio.domain.TradeType;
import com.stockmatch.portfolio.dto.TransactionCreateRequest;
import com.stockmatch.portfolio.dto.TransactionResponse;
import com.stockmatch.portfolio.service.TransactionService;
import com.stockmatch.user.member.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TransactionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureJson
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("매수 API 정상 응답")
    void buy_api_success() throws Exception {

        // given
        Long userId = 1L;
        Long portfolioId = 10L;
        Long securityId = 100L;

        TransactionCreateRequest request = new TransactionCreateRequest(
                securityId,
                new BigDecimal("3"),
                new BigDecimal("12000"),
                BigDecimal.ZERO,
                LocalDateTime.of(2025, 11, 17, 12, 0),
                "테스트 매수"
        );

        // 서비스가 리턴할 가짜 응답
        TransactionResponse response = new TransactionResponse(
                1L,
                portfolioId,
                securityId,
                TradeType.BUY,
                new BigDecimal("3"),
                new BigDecimal("12000"),
                BigDecimal.ZERO,
                LocalDateTime.of(2025, 11, 17, 12, 0),
                "테스트 매수"
        );

        User user = User.builder()
                .id(userId)
                .build();

        CustomUserDetails principal = new CustomUserDetails(user);

        given(transactionService.buy(
                eq(userId),
                eq(portfolioId),
                any(TransactionCreateRequest.class)
        )).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/portfolio/{portfolioId}/transaction/buy", portfolioId)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.type").value("BUY"))
                .andExpect(jsonPath("$.data.quantity").value(3));
    }
}
