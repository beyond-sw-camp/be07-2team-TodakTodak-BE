package com.padaks.todaktodak.notification.domain;

public enum Type {
//    회원 가입 시 토닥 admin에게 가는 알림(test용)
//    REGISTER,
//    예약 발생 시 병원 admin에게 가는 알림
    RESERVATION_NOTIFICATION,
//    대기 순번이 다가왔을 경우 예약자에게 가는 알림
    RESERVATION_WAITING,
//    게시글에 댓글이 달렸을 경우 게시글 작성자에게 가는 알림
    POST,
//    댓글에 대한 답글이 달렸을 경우 기존 댓글 작성자에게 가는 알림
    COMMENT,
//    게시글에 좋아요가 달렸을 경우 게시글 작성자에게 가는 알림
    LIKE,
//    결제 성공시 admin 에게 가는 알림
    PAYMENT_SUCCESS,
//    결제 실패시 admin 에게 가는 알림
    PAYMENT_FAIL,
//    결제 취소시 admin 에게 가는 알림
    PAYMENT_CANCEL,
//    결제 취소 실패시 admin 에게 가는 알림
    PAYMENT_CANCEL_FAIL,
//    CS 채팅에 대한 문의/답변이 생겼을때 가는 알림
    CHAT,
//    동일 자녀 등록시 먼저 자녀를 등록한 사용자에게 가는 알림
    REGISTER
}
