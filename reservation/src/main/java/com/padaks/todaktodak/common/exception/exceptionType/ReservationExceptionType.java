package com.padaks.todaktodak.common.exception.exceptionType;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RequiredArgsConstructor
public enum ReservationExceptionType implements ExceptionType{
    RESERVATION_NOT_FOUND(BAD_REQUEST, "해당 예약이 존재하지 않습니다."),
    MEDICALCHART_NOT_FOUND(BAD_REQUEST, "해당 진료내역이 존재하지 않습니다."),
    RESERVATION_DUPLICATE(BAD_REQUEST, "해당 의사선생님의 진료가 이미 예약되어 있습니다."),
    TOOMANY_RESERVATION(BAD_REQUEST, "금일 당일 예약이 관리자에 의해 종료되었습니다."),
    LOCK_OCCUPANCY(BAD_REQUEST, "이미 선택된 예약입니다."),
    REDIS_ERROR(BAD_REQUEST, "redisTemplate or opsForValue is null");

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus httpStatus(){
        return status;
    }

    @Override
    public String message(){
        return message;
    }
}
