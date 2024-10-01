//package com.padaks.todaktodak.notification.service;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.padaks.todaktodak.member.domain.Member;
//import com.padaks.todaktodak.member.repository.MemberRepository;
//import com.padaks.todaktodak.member.service.FcmService;
//import com.padaks.todaktodak.notification.domain.Type;
//import com.padaks.todaktodak.notification.dto.PaymentCancelDto;
//import com.padaks.todaktodak.notification.dto.PaymentCancelFailDto;
//import com.padaks.todaktodak.notification.dto.PaymentFailDto;
//import com.padaks.todaktodak.notification.dto.PaymentSuccessDto;
//import lombok.RequiredArgsConstructor;
//import com.fasterxml.jackson.core.type.TypeReference;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.support.Acknowledgment;
//import org.springframework.stereotype.Service;
//
//import javax.persistence.EntityNotFoundException;
//import java.math.BigDecimal;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class PaymentEventListener {
//
//    private final ObjectMapper objectMapper;
//    private final FcmService fcmService;
//    private final MemberRepository memberRepository;
//
//    @KafkaListener(topics = "payment-success", groupId = "payment-group", containerFactory = "payKafkaListenerContainerFactory")
//    public void listenPaymentSuccess(String message, Acknowledgment acknowledgment) throws JsonProcessingException {
//        try {
//            // JSON 문자열을 Map으로 변환
//            Map<String, Object> messageData = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
//            System.out.println("Received Payment Success message: " + messageData);
//
//            // 수신한 데이터를 처리하는 로직 추가
//            String memberEmail = (String) messageData.get("memberEmail");
//            Integer fee = (Integer) messageData.get("fee");
//            String name = (String) messageData.get("name");
//            Long adminId = 1L;
//
//            System.out.println("Email: " + memberEmail + ", Fee: " + fee + ", Name: " + name);
//
//            PaymentSuccessDto paymentSuccessDto = objectMapper.readValue(message, PaymentSuccessDto.class);
//            System.out.println("Received Payment Success DTO: " + paymentSuccessDto);
//
//            // 메시지 처리 후 수동 오프셋 커밋
//            fcmService.sendMessage(adminId, "회원 : " + name + " 결제 성공 알림", memberEmail+"님께서 "+fee+"원 결제하였습니다.", Type.PAYMENT_SUCCESS);
//
//            acknowledgment.acknowledge();
//        } catch (Exception e) {
//            System.err.println("Error processing payment success message: " + e.getMessage());
//        }
//    }
//
//
//    @KafkaListener(topics = "payment-fail", groupId = "payment-group", containerFactory = "payKafkaListenerContainerFactory")
//    public void listenPaymentFail(String message, Acknowledgment acknowledgment) throws JsonProcessingException {
//
//        PaymentFailDto paymentFailDto = objectMapper.readValue(message, PaymentFailDto.class);
//        System.out.println("Received Payment Fail DTO: " + paymentFailDto);
//        String memberEmail = paymentFailDto.getMemberEmail();
//        BigDecimal fee = paymentFailDto.getFee();
//
//        // 결제 실패 후 알림을 보낼 로직
//        Long adminId = 1L;
//        fcmService.sendMessage(adminId, "결제 실패 알림", memberEmail+"님의 "+fee+"원 결제가 실패하였습니다.", Type.PAYMENT_FAIL);
//
//        // 오프셋 변경
//        acknowledgment.acknowledge();
//    }
//
//    @KafkaListener(topics = "payment-cancel", groupId = "payment-group", containerFactory = "payKafkaListenerContainerFactory")
//    public void listenPaymentCancel(String message, Acknowledgment acknowledgment) throws JsonProcessingException {
//        System.out.println("Received Payment Cancel message: " + message);
//
//        PaymentCancelDto paymentCancelDto = objectMapper.readValue(message, PaymentCancelDto.class);
//        System.out.println("Received Payment Cancel DTO: " + paymentCancelDto);
//        // 결제 취소 후 알림을 보낼 로직
//        String memberEmail = paymentCancelDto.getMemberEmail();
//        BigDecimal fee = paymentCancelDto.getFee();
//        String name = paymentCancelDto.getName();
//        Long adminId = 1L;
//        fcmService.sendMessage(adminId, "회원 : " + name + " 결제 취소 성공 알림", memberEmail+"님의 "+fee+"원 결제가 취소되었습니다.", Type.PAYMENT_CANCEL);
//
//        // 오프셋 변경
//        acknowledgment.acknowledge();
//    }
//
//    @KafkaListener(topics = "payment-cancel-fail", groupId = "payment-group", containerFactory = "payKafkaListenerContainerFactory")
//    public void listenPaymentCancelFail(String message, Acknowledgment acknowledgment) throws JsonProcessingException {
//        System.out.println("Received Payment Cancel message: " + message);
//
//        PaymentCancelFailDto paymentCancelFailDto = objectMapper.readValue(message, PaymentCancelFailDto.class);
//        System.out.println("Received Payment Cancel Fail DTO: " + paymentCancelFailDto);
//        // 결제 취소 후 알림을 보낼 로직
//        String memberEmail = paymentCancelFailDto.getMemberEmail();
//        BigDecimal fee = paymentCancelFailDto.getFee();
//        Long adminId = 1L;
//        fcmService.sendMessage(adminId, "결제 취소 실패 알림", memberEmail+"님의 "+fee+"원 결제 취소가 실패하였습니다.", Type.PAYMENT_CANCEL_FAIL);
//
//        // 오프셋 변경
//        acknowledgment.acknowledge();
//    }
//}
