package com.stockmatch.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.config.jwt.JwtAuthenticationFilter;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.portfolio.dto.*;
import com.stockmatch.portfolio.service.HoldingService;
import com.stockmatch.portfolio.service.PortfolioService;
import com.stockmatch.portfolio.service.PortfolioValuationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PortfolioController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PortfolioControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    // ===== 서비스 Mock =====
    @MockitoBean
    private PortfolioService portfolioService;
    @MockitoBean
    private PortfolioValuationService portfolioValuationService;
    @MockitoBean
    private HoldingService holdingService;

    // ====== 인프라 Mock =====
    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    // 테스트 끝나면 인증 정보 비워주기
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("내 포트폴리오 생성/조회 성공")
    void ensureMyPortfolio_ok() throws Exception {
        // given
        long userId = 1L;

        CustomUserDetails principal = TestAuth.mockUserDetails(userId);
        given(portfolioService.ensureForUser(userId)).willReturn(mock(PortfolioResponse.class));

        // when
        ResultActions result = mockMvc.perform(post("/api/portfolio/me/ensure")
                .with(request -> {
                    setAuth(principal);
                    return request;
                }));

        // then
        result.andExpect(status().isOk());

        verify(portfolioService).ensureForUser(userId);
    }

    @Test
    @DisplayName("내 보유종목 추가/수정 성공")
    void addMyHolding_ok() throws Exception {
        // given
        long userId = 1L;

        CustomUserDetails principal = TestAuth.mockUserDetails(userId);

        HoldingRequest holdingRequest = new HoldingRequest(
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("123.45")
        );

        given(holdingService.addOrUpdateHolding(eq(userId), any(HoldingRequest.class)))
                .willReturn(mock(HoldingResponse.class));

        // when
        ResultActions result = mockMvc.perform(post("/api/portfolio/me/holdings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(holdingRequest))
                .with(request -> {
                    setAuth(principal);
                    return request;
                }));

        // then
        result.andExpect(status().isOk());

        ArgumentCaptor<HoldingRequest> captor = ArgumentCaptor.forClass(HoldingRequest.class);
        verify(holdingService).addOrUpdateHolding(eq(userId), captor.capture());

        HoldingRequest captured = captor.getValue();
        assertThat(captured.ticker()).isEqualTo("AAPL");
        assertThat(captured.quantity()).isEqualByComparingTo("10");
        assertThat(captured.avgPrice()).isEqualByComparingTo("123.45");
    }

    @Test
    @DisplayName("내 보유종목 목록 조회 성공")
    void getMyHoldings_ok() throws Exception {
        // given
        long userId = 1L;

        CustomUserDetails principal = TestAuth.mockUserDetails(userId);
        given(holdingService.getMyHoldings(userId))
                .willReturn(List.of(mock(HoldingResponse.class)));

        // when
        ResultActions result = mockMvc.perform(get("/api/portfolio/me/holdings")
                .with(request -> {
                    setAuth(principal);
                    return request;
                }));

        // then
        result.andExpect(status().isOk());

        verify(holdingService).getMyHoldings(userId);
    }

    @Test
    @DisplayName("내 보유종목 삭제 성공")
    void deleteMyHolding_ok() throws Exception {
        // given
        long userId = 1L;
        long holdingId = 10L;

        CustomUserDetails principal = TestAuth.mockUserDetails(userId);

        // when
        ResultActions result = mockMvc.perform(delete("/api/portfolio/me/holdings/{holdingId}", holdingId)
                .with(request -> {
                    setAuth(principal);
                    return request;
                }));

        // then
        result.andExpect(status().isOk());

        verify(holdingService).deleteMyHolding(userId, holdingId);
    }

    @Test
    @DisplayName("내 포트폴리오 평가 조회 성공")
    void getMyPortfolioValuation_ok() throws Exception {
        // given
        long userId = 1L;
        long portfolioId = 123L;

        CustomUserDetails principal = TestAuth.mockUserDetails(userId);

        PortfolioResponse portfolio = mock(PortfolioResponse.class);
        when(portfolio.getPortfolioId()).thenReturn(portfolioId);

        given(portfolioService.ensureForUser(userId)).willReturn(portfolio);
        given(portfolioValuationService.calculate(portfolioId)).willReturn(mock(PortfolioValuationResponse.class));

        // when
        ResultActions result = mockMvc.perform(get("/api/portfolio/me/valuation")
                .with(request -> {
                    setAuth(principal);
                    return request;
                }));

        // then
        result.andExpect(status().isOk());

        verify(portfolioService).ensureForUser(userId);
        verify(portfolioValuationService).calculate(portfolioId);
    }

    @Test
    @DisplayName("내 포트폴리오 일별 평가 조회 성공")
    void getMyPortfolioDailyValuation_ok() throws Exception {
        // given
        long userId = 1L;
        long portfolioId = 123L;

        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        CustomUserDetails principal = TestAuth.mockUserDetails(userId);

        PortfolioResponse portfolio = mock(PortfolioResponse.class);
        when(portfolio.getPortfolioId()).thenReturn(portfolioId);

        given(portfolioService.ensureForUser(userId))
                .willReturn(portfolio);
        given(portfolioValuationService.calculateDailyHistory(portfolioId, from, to))
                .willReturn(List.of(mock(PortfolioDailySummaryResponse.class)));

        // when
        ResultActions result = mockMvc.perform(get("/api/portfolio/me/valuation/daily")
                .param("from", from.toString())
                .param("to", to.toString())
                .with(request -> {
                    setAuth(principal);
                    return request;
                }));

        // then
        result.andExpect(status().isOk());

        verify(portfolioService).ensureForUser(userId);
        verify(portfolioValuationService).calculateDailyHistory(portfolioId, from, to);
    }


    // ===== 테스트 인증 헬퍼 =====
    static class TestAuth {
        static CustomUserDetails mockUserDetails(long userId) {
            CustomUserDetails customUserDetails = mock(CustomUserDetails.class, RETURNS_DEEP_STUBS);

            when(customUserDetails.getUser().getId()).thenReturn(userId);

            doReturn(List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_USER")))
                    .when(customUserDetails).getAuthorities();

            return customUserDetails;
        }
    }

    // ===== 공통 헬퍼 메서드 =====
    private void setAuth(CustomUserDetails principal) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                principal.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
