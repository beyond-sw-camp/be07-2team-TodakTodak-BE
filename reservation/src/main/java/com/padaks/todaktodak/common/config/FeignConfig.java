package com.padaks.todaktodak.common.config;

//import feign.RequestInterceptor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpHeaders;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//@Configuration
//public class FeignConfig {
//    //token 작업 공동화
//    @Bean
//    public RequestInterceptor requestInterceptor(){
//        return request -> {
//            //모든 feign 요청에 전역적으로 token을 세팅할 수 있음
//            String token = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
//            request.header(HttpHeaders.AUTHORIZATION, "Bearer "+token);
//        };
//    }
//}

import feign.RequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignConfig {

    private static final Logger log = LoggerFactory.getLogger(FeignConfig.class);

    // token 작업 공동화
    @Bean
    public RequestInterceptor requestInterceptor() {
        return request -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() != null) {
                String token = (String) authentication.getCredentials();
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            } else {
                // 인증되지 않은 경우 로깅
                log.warn("No authentication found for Feign request.");
            }
        };
    }
}
