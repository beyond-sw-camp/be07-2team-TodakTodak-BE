package com.padaks.todaktodak.common.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

@Configuration
public class FeignConfig {
    //token 작업 공동화
    @Bean
    public RequestInterceptor requestInterceptor(){
        return request -> {
            // SecurityContextHolder에서 인증 정보를 가져와 토큰을 설정
            if (SecurityContextHolder.getContext().getAuthentication() != null &&
                    SecurityContextHolder.getContext().getAuthentication().getCredentials() != null) {
                String token = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
                if (StringUtils.hasText(token)) {
                    request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                }
            }
        };
    }
}
