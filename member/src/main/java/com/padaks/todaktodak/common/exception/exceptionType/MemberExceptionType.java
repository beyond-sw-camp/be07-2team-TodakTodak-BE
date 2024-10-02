package com.padaks.todaktodak.common.exception.exceptionType;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RequiredArgsConstructor
public enum MemberExceptionType implements ExceptionType{
    MEMBER_NOT_FOUND(BAD_REQUEST, "회원이 존재하지 않습니다."),
    CHILD_NOT_FOUND(BAD_REQUEST, "해당 자녀가 존재하지 않습니다."),
    TODAK_ADMIN_ONLY(BAD_REQUEST, "접근제한 : todak admin만 접근 가능");

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus httpStatus(){
        return null;
    }

    @Override
    public String message(){
        return null;
    }
}
