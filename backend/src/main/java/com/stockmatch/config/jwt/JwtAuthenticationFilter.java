package com.stockmatch.config.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.member.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public final JwtUtil jwtUtil;
    public final UserRepository userRepository;
    public final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain
    ) throws ServletException, IOException {

        // 헤더에서 토큰 추출
        String accessToken = resolveToken(request);
        log.info("검증 시작 - 토큰: {}", accessToken); // 조작된 토큰이 맞는지 확인

        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/api/auth/callback") ||
            requestURI.equals("/api/auth/refresh") ||
            requestURI.equals("/favicon.ico")) {

            filterChain.doFilter(request, response);

            return;
        }
        // 토큰이 없으면 다음 필터로 진행 (인증이 필요없는 페이지 접근)
        if (accessToken == null) {
            filterChain.doFilter(request, response);

            return;
        }

        try {
            // AccessToken 유효성 검사
            jwtUtil.validateTokenOrThrow(accessToken);
            log.info("검증 통과함 - 이 로그가 보이면 안 됨(조작 시)");

            // 토큰에서 userPk 추출
            String userPk = jwtUtil.getUserPkFromToken(accessToken);

            // DB에서 사용자 조회
            User user = userRepository.findById(Long.parseLong(userPk))
                    .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

            // CustomUserDetails 객체 생성
            CustomUserDetails userDetails = new CustomUserDetails(user);

            // Spring security 인증 토큰 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, // 인증된 사용자 정보
                            null, // 비밀번호 사용 x
                            userDetails.getAuthorities() // 권한 목록
                    );

            // 요청에 대한 부가 정보 설정
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            //SecurityContext에 인증정보저장
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ExpiredJwtException e){
            log.error("토큰 만료됨 (401 전송 시도)");
            setErrorResponse(response, ErrorCode.UNAUTHORIZED);
            return;
        } catch (BusinessException e) {
            log.error("검증 실패로 차단됨: {}", e.getMessage());
            setErrorResponse(response, e.getErrorCode());
            return;
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication", e);
            setErrorResponse(response, ErrorCode.INTERNAL_SERVER_ERROR);
            return;
        }

        filterChain.doFilter(request,response);

    }

    private String resolveToken(HttpServletRequest request){
        //헤더에서 authorization 값 가져옴
        String bearerToken = request.getHeader("Authorization");
        // 텍스트가 있는지와 "Bearer "로 시작하는지 확인
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){
            //7번 인덱스부터 끝까지의 문자열을 잘라내어 반환
            return bearerToken.substring(7);
        }

       //헤더에 없다면 쿠키에서 accessToken 확인
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if(cookies != null ){
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }


        return null;
    }

    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> errorResponse = ApiResponse.fail(errorCode.code(), errorCode.message());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
