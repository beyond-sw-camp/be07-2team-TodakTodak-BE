package com.padaks.todaktodak.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.padaks.todaktodak.common.dto.DtoMapper;
import com.padaks.todaktodak.common.exception.BaseException;
import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.repository.MemberRepository;
import com.padaks.todaktodak.member.service.FcmService;
import com.padaks.todaktodak.notification.domain.Notification;
import com.padaks.todaktodak.notification.domain.Type;
import com.padaks.todaktodak.notification.dto.*;
import com.padaks.todaktodak.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.sql.Struct;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {
    private final RedisTemplate<String, Object> redisTemplate;
    private final MemberRepository memberRepository;
    private final FcmService fcmService;
    private final ObjectMapper objectMapper;

    //자녀 등록 알림
    @KafkaListener(topics = "child-share", groupId = "group_id", containerFactory = "kafkaListenerContainerFactory")
    public void registerNotification(String message) {
        System.out.println("==========여기!!!!!============호출됨!!!!!!!==========");
        try {
            // 이스케이프 제거
            String cleanMessage = message.replace("\\\"", "\"").replace("\\\\", "\\"); // 이스케이프 처리
            cleanMessage = cleanMessage.replaceAll("^\"|\"$", ""); // 문자열 양 끝의 쌍따옴표 제거

            // JSON 문자열을 Map으로 변환
            System.out.println("이스케이프 제거한 message");
            System.out.println(cleanMessage);
            Map<String, Object> messageData = objectMapper.readValue(cleanMessage, new TypeReference<Map<String, Object>>() {});
            System.out.println("Received Payment Success message: " + messageData);

            // 수신한 데이터를 처리하는 로직 추가
            String memberEmail = (String) messageData.get("memberEmail");
            Long childId = ((Number) messageData.get("childId")).longValue(); // Number로 캐스팅 후 Long으로 변환
            String name = (String) messageData.get("childName");
            String sharer = (String) messageData.get("sharer");

            // 형식 확인용
            System.out.println("수신 Email: " + memberEmail + ", childId: " + childId + ", child name: " + name + ", 공유자 이름: " + sharer);

            Member receiver = memberRepository.findByMemberEmail(memberEmail)
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));

            fcmService.sendMessage(receiver.getId(), "자녀 등록 알림", sharer + " 사용자가 자녀 " + name + "를 등록하였습니다.", Type.REGISTER, childId);

        } catch (JsonProcessingException e) {
            System.err.println("JSON 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("알 수 없는 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 커뮤니티 알림
    @KafkaListener(topics = "community-success", groupId = "group_id", containerFactory = "kafkaListenerContainerFactory")
    public void consumerNotification(String message) throws JsonProcessingException {
        // 이스케이프 제거
        String cleanMessage = message.replace("\\\"", "\"").replace("\"{", "{").replace("}\"", "}");

        // JSON을 Map으로 변환
        Map<String, Object> messageData = objectMapper.readValue(cleanMessage, new TypeReference<Map<String, Object>>() {});
        System.out.println("Received community Success message: " + messageData);

        String memberEmail = (String) messageData.get("receiverEmail");
        // postId를 Integer로 읽고 Long으로 변환
        Long postId = ((Integer) messageData.get("postId")).longValue();
        String title = (String) messageData.get("title");
        Type type = Type.valueOf((String) messageData.get("type"));  // Enum 타입 변환

        Member member = memberRepository.findByMemberEmail(memberEmail)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));

        String category = "";
        if (type.equals(Type.POST)) {
            category = "질문 알림";
            fcmService.sendMessage(member.getId(), category, "질문 :" + title + "에 대한 답변이 작성되었습니다.", Type.POST, postId);
        } else if (type.equals(Type.COMMENT)) {
            category = "답변 알림";
            String comment = "에 작성한 답변";
            fcmService.sendMessage(member.getId(), category, "질문 :" + title + comment + "에 대한 답변이 작성되었습니다.", Type.COMMENT, postId);
        }
    }



    //좋아요 알림

    // 결제 알림
    @KafkaListener(topics = "payment-success", groupId = "payment-group", containerFactory = "payKafkaListenerContainerFactory")
    public void listenPaymentSuccess(String message, Acknowledgment acknowledgment) throws JsonProcessingException {
        try {
            // JSON 문자열을 Map으로 변환
            Map<String, Object> messageData = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
            System.out.println("Received Payment Success message: " + messageData);

            // 수신한 데이터를 처리하는 로직 추가
            String memberEmail = (String) messageData.get("memberEmail");
            Integer fee = (Integer) messageData.get("fee");
            String name = (String) messageData.get("name");
            String hospitalAdminEmail = (String) messageData.get("hospitalAdmin");

            System.out.println("Email: " + memberEmail + ", Fee: " + fee + ", Name: " + name);

//            PaymentSuccessDto paymentSuccessDto = objectMapper.readValue(message, PaymentSuccessDto.class);
//            System.out.println("Received Payment Success DTO: " + paymentSuccessDto);

            Member member = memberRepository.findByMemberEmail(memberEmail).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 회원입니다."));
            Member hospitalAdmin = memberRepository.findByMemberEmail(hospitalAdminEmail).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 병원 admin입니다."));
            // 메시지 처리 후 수동 오프셋 커밋
            //병원에서 정기 결제 했을 때 admin에게 가는 알림
            Long adminId = 1L;
            fcmService.sendMessage(adminId, "회원 : " + name + " 결제 성공 알림", memberEmail+"님께서 "+fee+"원 결제하였습니다.", Type.PAYMENT_SUCCESS, null);

            fcmService.sendMessage(hospitalAdmin.getId(), "회원 : " + name + " 결제 성공 알림", memberEmail+"님께서 "+fee+"원 결제하였습니다.", Type.PAYMENT_SUCCESS, null);

            //결제자에게 메세지 전송
            fcmService.sendMessage(member.getId(), "회원 : " + name + " 결제 성공 알림", memberEmail+"님께서 "+fee+"원 결제하였습니다.", Type.PAYMENT_SUCCESS, null);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            System.err.println("Error processing payment success message: " + e.getMessage());
        }
    }


    @KafkaListener(topics = "payment-fail", groupId = "payment-group", containerFactory = "payKafkaListenerContainerFactory")
    public void listenPaymentFail(String message, Acknowledgment acknowledgment) throws JsonProcessingException {

//        PaymentFailDto paymentFailDto = objectMapper.readValue(message, PaymentFailDto.class);
//        System.out.println("Received Payment Fail DTO: " + paymentFailDto);
//        String memberEmail = paymentFailDto.getMemberEmail();
//        BigDecimal fee = paymentFailDto.getFee();
        Map<String, Object> messageData = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
        System.out.println("Received Payment fail message: " + messageData);

        // 수신한 데이터를 처리하는 로직 추가
        String memberEmail = (String) messageData.get("memberEmail");
        Integer fee = (Integer) messageData.get("fee");
        String name = (String) messageData.get("name");
        String hospitalAdmin = (String)messageData.get("hospitalAdmin");

        System.out.println("Email: " + memberEmail + ", Fee: " + fee + ", Name: " + name);
        Member member = memberRepository.findByMemberEmail(memberEmail).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 회원입니다."));
        Member hospital = memberRepository.findByMemberEmail(hospitalAdmin).orElseThrow(()->new EntityNotFoundException("존재하지 않는 병원 관리자입니다."));
        // 결제 실패 후 알림을 보낼 로직
        // 정기 결제 실패
        Long adminId = 1L;
        fcmService.sendMessage(adminId, "결제 실패 알림", "회원 : " +memberEmail+"님의 "+fee+"원 결제가 실패하였습니다.", Type.PAYMENT_FAIL, null);

        //병원 admin 메세지 전송
        fcmService.sendMessage(hospital.getId(), "회원 : " + name + " 결제 실패 알림", memberEmail+"님의 "+fee+"원 결제가 실패하였습니다..", Type.PAYMENT_FAIL, null);

        //결제자에게 메세지 전송
        fcmService.sendMessage(member.getId(), "회원 : " + name + " 결제 실패 알림", memberEmail+"님의 "+fee+"원 결제가 실패하였습니다..", Type.PAYMENT_FAIL, null);


        // 오프셋 변경
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "payment-cancel", groupId = "payment-group", containerFactory = "payKafkaListenerContainerFactory")
    public void listenPaymentCancel(String message, Acknowledgment acknowledgment) throws JsonProcessingException {

//        PaymentCancelDto paymentCancelDto = objectMapper.readValue(message, PaymentCancelDto.class);
//        System.out.println("Received Payment Cancel DTO: " + paymentCancelDto);
//        // 결제 취소 후 알림을 보낼 로직
//        String memberEmail = paymentCancelDto.getMemberEmail();
//        BigDecimal fee = paymentCancelDto.getFee();
//        String name = paymentCancelDto.getName();
        Map<String, Object> messageData = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
        System.out.println("Received Payment cancel message: " + messageData);

        // 수신한 데이터를 처리하는 로직 추가
        String memberEmail = (String) messageData.get("memberEmail");
        Integer fee = (Integer) messageData.get("fee");
        String name = (String) messageData.get("name");
        String hospitalAdminEmail = (String) messageData.get("hospitalAdmin");

        System.out.println("Email: " + memberEmail + ", Fee: " + fee + ", Name: " + name);
        Member member = memberRepository.findByMemberEmail(memberEmail).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 회원입니다."));
        Member hospitalAdmin = memberRepository.findByMemberEmail(hospitalAdminEmail).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 병원 admin입니다."));

        //파닥 admin 에게 전송
        Long adminId = 1L;
        fcmService.sendMessage(adminId, " 결제 취소 성공 알림", memberEmail+"님의 "+fee+"원 결제가 취소되었습니다.", Type.PAYMENT_CANCEL, null);

        //병원에 메세지 전송
        fcmService.sendMessage(hospitalAdmin.getId(), "회원 : " + name + " 결제 취소 알림", memberEmail+"님의 "+fee+"원 결제가 취소되었습니다.", Type.PAYMENT_CANCEL, null);

        //결제자에게 메세지 전송
        fcmService.sendMessage(member.getId(), "회원 : " + name + " 결제 취소 알림", memberEmail+"님의 "+fee+"원 결제가 취소되었습니다.", Type.PAYMENT_CANCEL, null);

        // 오프셋 변경
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "payment-cancel-fail", groupId = "payment-group", containerFactory = "payKafkaListenerContainerFactory")
    public void listenPaymentCancelFail(String message, Acknowledgment acknowledgment) throws JsonProcessingException {
//        System.out.println("Received Payment cancel fail message: " + message);
//
//        PaymentCancelFailDto paymentCancelFailDto = objectMapper.readValue(message, PaymentCancelFailDto.class);
//        System.out.println("Received Payment Cancel Fail DTO: " + paymentCancelFailDto);
//        // 결제 취소 후 알림을 보낼 로직
//        String memberEmail = paymentCancelFailDto.getMemberEmail();
//        BigDecimal fee = paymentCancelFailDto.getFee();
        Map<String, Object> messageData = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
        System.out.println("Received Payment cancel fail message: " + messageData);

        // 수신한 데이터를 처리하는 로직 추가
        String memberEmail = (String) messageData.get("memberEmail");
        Integer fee = (Integer) messageData.get("fee");
        String name = (String) messageData.get("name");
        String hospitalAdminEmail = (String) messageData.get("hospitalAdmin");

        System.out.println("Email: " + memberEmail + ", Fee: " + fee + ", Name: " + name);
        Member member = memberRepository.findByMemberEmail(memberEmail).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 회원입니다."));
        Member hospitalAdmin = memberRepository.findByMemberEmail(hospitalAdminEmail).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 병원 admin입니다."));

        //결제 취소 실패 admin 알림
        Long adminId = 1L;
        fcmService.sendMessage(adminId, "결제 취소 실패 알림", memberEmail+"님의 "+fee+"원 결제 취소가 실패하였습니다.", Type.PAYMENT_CANCEL_FAIL, null);

        //결제자에게 메세지 전송
        fcmService.sendMessage(hospitalAdmin.getId(), "회원 : " + name + " 결제 취소 실패 알림", memberEmail+"님의 "+fee+"원 결제취소가 실패되었습니다.", Type.PAYMENT_CANCEL_FAIL, null);

        //결제자에게 메세지 전송
        fcmService.sendMessage(member.getId(), "회원 : " + name + " 결제 취소 실패 알림", memberEmail+"님의 "+fee+"원 결제취소가 실패되었습니다.", Type.PAYMENT_CANCEL_FAIL, null);

        // 오프셋 변경
        acknowledgment.acknowledge();
    }

    //병원 당일 예약 알림
    @KafkaListener(topics = "reservationImmediate", groupId = "Schedule_id", containerFactory = "ppKafkaListenerContainerFactory")
    public void immediateReservationNotification(String message) throws JsonProcessingException {
        String cleanMessage = message.replace("\\\"", "\"").replace("\"{", "{").replace("}\"", "}");

        // JSON을 Map으로 변환
        Map<String, Object> messageData = objectMapper.readValue(cleanMessage, new TypeReference<Map<String, Object>>() {});
        System.out.println("Received Reservation Success message: " + messageData);

        ReservationSaveReqDto dto = objectMapper.readValue(cleanMessage, ReservationSaveReqDto.class);

        String memberEmail = dto.getMemberEmail();
        Member member = memberRepository.findByMemberEmail(memberEmail).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 회원입니다."));
        Member doctor = memberRepository.findByMemberEmail(dto.getDoctorEmail()).orElseThrow(()->new EntityNotFoundException("존재하지 않는 의사입니다."));
        String reservationType = dto.getReservationType();
        String medicalItem = dto.getMedicalItem();  //일반진료, 예방접종 ..
        Long childId = dto.getChildId();

        System.out.println("========================================");
//        System.out.println(memberEmail);
        System.out.println(member.getId());
        System.out.println(doctor.getId());
        System.out.println("========================================");
        //예약자에게 전송
        fcmService.sendMessage(member.getId(), reservationType+"예약 성공 알림",
                        "예약구분 : " + medicalItem + "\n"
                        +doctor.getName()+"선생님 진료예약되었습니다.",Type.RESERVATION_NOTIFICATION, childId);

        //의사에게 전송
        fcmService.sendMessage(doctor.getId(), reservationType+"예약 성공 알림",
                 "예약구분 : " + medicalItem + "\n"
                        + member.getName()+"님 진료예약되었습니다.",Type.RESERVATION_NOTIFICATION, childId);
    }
}


