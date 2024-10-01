package com.padaks.todaktodak.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.padaks.todaktodak.common.dto.MemberFeignDto;
import com.padaks.todaktodak.common.feign.MemberFeignClient;
import com.padaks.todaktodak.medicalchart.domain.MedicalChart;
import com.padaks.todaktodak.medicalchart.repository.MedicalChartRepository;
import com.padaks.todaktodak.payment.domain.Pay;
import com.padaks.todaktodak.payment.domain.PaymentMethod;
import com.padaks.todaktodak.payment.dto.PaymentListResDto;
import com.padaks.todaktodak.payment.dto.PaymentReqDto;
import com.padaks.todaktodak.payment.domain.PaymentStatus;
import com.padaks.todaktodak.payment.repository.PaymentRepository;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.request.AgainPaymentData;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private final MemberFeignClient memberFeignClient;
    private final IamportClient iamportClient;
    private final PaymentRepository paymentRepository;
    private final MedicalChartRepository medicalChartRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    public static MedicalChart medicalChart;


    // member 객체 리턴, 토큰 포함
    public MemberFeignDto getMemberInfo() {
        MemberFeignDto member = memberFeignClient.getMemberEmail();  // Feign Client에 토큰 추가
        return member;
    }

    // 진료 내역 객체 리턴
    public MedicalChart getMediChartId(Long id){
        medicalChart = medicalChartRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 진료내역입니다."));
        return medicalChart;
    }

    // 단건 결제 처리
    public PaymentReqDto processSinglePayment(String impUid) throws Exception {
        return processPayment(impUid, PaymentMethod.SINGLE);
    }

    // 정기 결제 처리
    public PaymentReqDto processSubscriptionPayment(String impUid) throws Exception {
        return subPayment(impUid, PaymentMethod.SUBSCRIPTION);
    }

    // impUid를 파싱하는 메서드
    private String extractImpUid(String impUidJson) {
        try {
            // JSON 파싱을 통해 impUid 값 추출 (예: Jackson 사용)
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> map = objectMapper.readValue(impUidJson, Map.class);
            return map.get("impUid");
        } catch (Exception e) {
            throw new RuntimeException("impUid 값을 추출하는 중 오류 발생", e);
        }
    }

    // 단건 결제 로직 구현
    public PaymentReqDto processPayment(String impUid, PaymentMethod paymentMethod) throws Exception {
        MemberFeignDto member = getMemberInfo();  // 현재 로그인한 사용자 정보
//        System.out.println(impUid);

        int fee = medicalChart.getFee();
        String hospitalAdmin = medicalChart.getReservation().getHospital().getAdminEmail();

        String actualImpUid = extractImpUid(impUid);  // impUid 값 추출 함수 사용
//        System.out.println("Extracted impUid: " + actualImpUid);

        // impUid를 통해 결제 정보 확인
        IamportResponse<Payment> paymentResponse = iamportClient.paymentByImpUid(actualImpUid);

        if (paymentResponse.getResponse() == null) {
            throw new Exception("결제 정보 없음: " + paymentResponse.getMessage());
        }

        BigDecimal amount = paymentResponse.getResponse().getAmount();
        if (amount.compareTo(BigDecimal.valueOf(fee)) != 0) {
            throw new Exception("결제 금액 불일치");
        }

        try {
            // 결제 정보가 존재하는지 확인
            if (paymentResponse.getResponse() == null) {
                throw new Exception("결제 요청 실패: " + paymentResponse.getMessage());
            }

            // 결제 금액 및 기타 검증 로직 (여기서는 금액 100원으로 고정)
            if (paymentResponse.getResponse().getAmount().intValue() != fee) {
                throw new Exception("결제 금액 불일치");
            }
            String customerUid = "customer_" + member.getMemberEmail();
            Pay pay = null;

            String name = "비대면 진료";
            // Pay 엔티티 생성 후 저장
            pay = Pay.builder()
                    .memberEmail(member.getMemberEmail())
                    .impUid(actualImpUid)
                    .customerUid(customerUid)
                    .amount(BigDecimal.valueOf(fee))
                    .buyerName(member.getName())
                    .name(name)
                    .buyerTel(member.getPhoneNumber())
                    .merchantUid("order_no_" + new Date().getTime())
                    .paymentStatus(PaymentStatus.OK)  // 결제 완료로 상태 업데이트
                    .paymentMethod(paymentMethod)
                    .medicalChart(medicalChart)
                    .requestTimeStamp(LocalDateTime.now())
                    .approvalTimeStamp(LocalDateTime.now())  // 결제 승인 시간
                    .subscriptionEndDate(null)  // 정기결제의 경우 다음 결제일을 1개월 후로 설정
                    .count(0)
                    .build();

            // 저장 로직
            paymentRepository.save(pay);
            String memberEmail = pay.getMemberEmail();


            // 메시지 데이터 객체 생성
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("memberEmail", memberEmail);
            messageData.put("fee", fee);
            messageData.put("name", name);
            messageData.put("hospitalAdmin", hospitalAdmin);

            // 객체를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(messageData);

            // Kafka로 메시지 전송
            kafkaTemplate.send("payment-success", message);
            log.info("결제 성공 메시지를 Kafka로 전송: {}", message);

            // 성공적으로 결제된 PaymentReqDto 반환
            return PaymentReqDto.builder()
                    .id(pay.getId())
                    .memberEmail(pay.getMemberEmail())
                    .customerUid(pay.getCustomerUid())
                    .impUid(pay.getImpUid())
                    .amount(pay.getAmount())
                    .merchantUid(pay.getMerchantUid())
                    .buyerName(pay.getBuyerName())
                    .name(pay.getName())
                    .buyerTel(pay.getBuyerTel())
                    .paymentStatus(pay.getPaymentStatus().toString())
                    .paymentMethod(pay.getPaymentMethod().toString())
                    .requestTimeStamp(pay.getRequestTimeStamp())
                    .approvalTimeStamp(pay.getApprovalTimeStamp())
                    .build();

        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생: ", e);

            String memberEmail = member.getMemberEmail();
            // 메시지 데이터 객체 생성
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("memberEmail", memberEmail);
            messageData.put("fee", fee);
            messageData.put("impUid", impUid);
            messageData.put("hospitalAdmin", hospitalAdmin);

            // 객체를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(messageData);

            // Kafka로 메시지 전송
            kafkaTemplate.send("payment-fail", message);
            log.info("결제 성공 메시지를 Kafka로 전송: {}", message);
            throw new RuntimeException("결제 처리 실패", e);
        }
    }

    // 정기 결제 로직
    public PaymentReqDto subPayment(String impUid, PaymentMethod paymentMethod) throws Exception {
        MemberFeignDto member = getMemberInfo();  // 현재 로그인한 사용자 정보
//        System.out.println(impUid);

        String actualImpUid = extractImpUid(impUid);  // impUid 값 추출 함수 사용
//        System.out.println("Extracted impUid: " + actualImpUid);

        String hospitalAdmin = medicalChart.getReservation().getHospital().getAdminEmail();
        // impUid를 통해 결제 정보 확인
        IamportResponse<Payment> paymentResponse = iamportClient.paymentByImpUid(actualImpUid);

        if (paymentResponse.getResponse() == null) {
            throw new Exception("결제 정보 없음: " + paymentResponse.getMessage());
        }

        BigDecimal amount = paymentResponse.getResponse().getAmount();
        if (amount.compareTo(BigDecimal.valueOf(1000000)) != 0) {
            throw new Exception("결제 금액 불일치");
        }

        try {
            // 결제 정보가 존재하는지 확인
            if (paymentResponse.getResponse() == null) {
                throw new Exception("결제 요청 실패: " + paymentResponse.getMessage());
            }

            // 결제 금액 및 기타 검증 로직 (여기서는 금액 100원으로 고정)
            if (paymentResponse.getResponse().getAmount().intValue() != 1000000) {
                throw new Exception("결제 금액 불일치");
            }
            String customerUid = "customer_" + member.getMemberEmail();
            Pay pay = null;

            String name = "정기 구독";

            // Pay 엔티티 생성 후 저장
            pay = Pay.builder()
                    .memberEmail(member.getMemberEmail())
                    .impUid(actualImpUid)
                    .customerUid(customerUid)
                    .amount(BigDecimal.valueOf(1000000))
                    .buyerName(member.getName())
                    .name(name)
                    .buyerTel(member.getPhoneNumber())
                    .merchantUid("order_no_" + new Date().getTime())
                    .paymentStatus(PaymentStatus.SUBSCRIBING)  // 결제 완료로 상태 업데이트
                    .paymentMethod(paymentMethod)
                    .requestTimeStamp(LocalDateTime.now())
                    .approvalTimeStamp(LocalDateTime.now())  // 결제 승인 시간
                    .subscriptionEndDate(LocalDateTime.now().plusMonths(1))
                    .count(0)
                    .build();

            // 저장 로직
            paymentRepository.save(pay);
            //String hospitalAdmin = pay.getMedicalChart().getReservation().getHospital().getAdminEmail();
            String memberEmail = pay.getMemberEmail();
            BigDecimal fee = pay.getAmount();

            // 메시지 데이터 객체 생성
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("memberEmail", memberEmail);
            messageData.put("fee", fee);
            messageData.put("name", name);
            messageData.put("hospitalAdmin", hospitalAdmin);

            // 객체를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(messageData);

            // Kafka로 메시지 전송
            kafkaTemplate.send("payment-success", message);
            log.info("결제 성공 메시지를 Kafka로 전송: {}", message);

            // 성공적으로 결제된 PaymentReqDto 반환
            return PaymentReqDto.builder()
                    .id(pay.getId())
                    .memberEmail(pay.getMemberEmail())
                    .customerUid(pay.getCustomerUid())
                    .impUid(pay.getImpUid())
                    .amount(pay.getAmount())
                    .merchantUid(pay.getMerchantUid())
                    .buyerName(pay.getBuyerName())
                    .name(pay.getName())
                    .buyerTel(pay.getBuyerTel())
                    .paymentStatus(pay.getPaymentStatus().toString())
                    .paymentMethod(pay.getPaymentMethod().toString())
                    .requestTimeStamp(pay.getRequestTimeStamp())
                    .approvalTimeStamp(pay.getApprovalTimeStamp())
                    .build();

        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생: ", e);
            String memberEmail = member.getMemberEmail();
            // 메시지 데이터 객체 생성
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("memberEmail", memberEmail);
            messageData.put("fee", amount);
            messageData.put("impUid", impUid);
            messageData.put("hospitalAdmin", hospitalAdmin);

            // 객체를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(messageData);

            // Kafka로 메시지 전송
            kafkaTemplate.send("payment-fail", message);
            log.error("결제 실패 메시지를 Kafka로 전송: {}", messageData);
            throw new RuntimeException("결제 처리 실패", e);
        }
    }

    // 결제 취소 메소드
    public IamportResponse<Payment> cancelPayment(String impUid) throws Exception {
        String actualImpUid = extractImpUid(impUid);  // impUid 값 추출 함수 사용

        // DB에서 결제 정보를 조회
        Pay pay = paymentRepository.findByImpUid(actualImpUid);
        if (pay == null) {
            throw new Exception("해당 impUid에 대한 결제 정보가 없습니다.");
        }

        // 아임포트 서버에서 결제 정보를 조회
        IamportResponse<Payment> paymentResponse = iamportClient.paymentByImpUid(actualImpUid);
        String hospitalAdmin = medicalChart.getReservation().getHospital().getAdminEmail();

        // 아임포트와 DB 저장 금액이 일치하는지 확인하는 로직
        if (paymentResponse.getResponse() != null) {
            BigDecimal iamportAmount = paymentResponse.getResponse().getAmount();
            if (pay.getAmount().compareTo(iamportAmount) != 0) {
                throw new Exception("DB에 저장된 결제 금액과 아임포트에서 확인된 금액이 일치하지 않습니다.");
            }
        } else {
            throw new Exception("아임포트에서 해당 결제 정보를 찾을 수 없습니다.");
        }

        // 결제 취소 요청(impuid 또는 merchantUid를 통해 취소할 수 있음
        CancelData cancelData = new CancelData(actualImpUid, true);
        IamportResponse<Payment> cancelResponse = iamportClient.cancelPaymentByImpUid(cancelData);


        if (cancelResponse.getResponse() != null) {
            log.info("결제 취소 성공: {}", cancelResponse.getResponse());

            // 결제 상태를 CANCEL !
            pay.cancelPaymentStatus(PaymentStatus.CANCEL);
            paymentRepository.save(pay);

            String memberEmail = pay.getMemberEmail();
            BigDecimal fee = pay.getAmount();
            String name = pay.getName();

            // 메시지 데이터 객체 생성
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("memberEmail", memberEmail);
            messageData.put("fee", fee);
            messageData.put("name", name);
            messageData.put("hospitalAdmin",hospitalAdmin);

            // 객체를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(messageData);

            // Kafka로 메시지 전송
            kafkaTemplate.send("payment-cancel", message);
            log.info("결제 취소 메시지를 Kafka로 전송: {}", message);

        } else {
            log.error("결제 취소 실패: {}", cancelResponse.getMessage());

            String memberEmail = pay.getMemberEmail();
            BigDecimal fee = pay.getAmount();
//            String name = pay.getName();
            String impUid2 = pay.getImpUid();
            // 메시지 데이터 객체 생성
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("memberEmail", memberEmail);
            messageData.put("fee", fee);
            messageData.put("impUid", impUid2);
            messageData.put("hospitalAdmin", hospitalAdmin);

            // 객체를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(messageData);

            // Kafka로 메시지 전송
            kafkaTemplate.send("payment-cancel-fail", message);
            log.error("결제 취소 실패 메시지를 Kafka로 전송: {}", message);
            throw new Exception("결제 취소 실패: " + cancelResponse.getMessage());
        }
        return cancelResponse;
    }

    // 결제 내역 리스트 조회
    public Page<PaymentListResDto> paymentList(Pageable pageable) {
        Page<Pay> pays = paymentRepository.findAll(pageable);
        return pays.map(a -> a.listFromEntity());
    }


    // 정기결제 상태 체크 및 다음 결제일 갱신
    public void processSubscriptions() {
        log.info("정기 결제 프로세스 시작");
//        List<Pay> subscriptionPayments = paymentRepository.findByPaymentMethod(PaymentMethod.SUBSCRIPTION);
        List<Pay> subscriptionPayments = paymentRepository.findByPaymentMethodAndPaymentStatus(
                PaymentMethod.SUBSCRIPTION, PaymentStatus.SUBSCRIBING);


        for (Pay pay : subscriptionPayments) {
            System.out.println(pay);
            System.out.println(pay.getPaymentStatus());
            // 정기결제 상태가 만료된 경우 즉, 구독이 만료된 경우! 아래 로직을 실행 ~
            if (pay.isSubscriptionActive()) {
                try {
                    String customerUid = pay.getCustomerUid();
                    String merchantUid = "subscription_" + pay.getMemberEmail() + "_" + LocalDateTime.now();

                    log.info("정기 결제 요청 시작: customerUid = {}, merchantUid = {}", customerUid, merchantUid);

                    // AgainPaymentData 객체 사용하여 정기 결제 요청 구성
                    AgainPaymentData againPaymentData = new AgainPaymentData(customerUid, merchantUid, pay.getAmount());
                    againPaymentData.setBuyerName(pay.getBuyerName());
                    againPaymentData.setName(pay.getName());

                    log.info("AgainPaymentData: customerUid = {}, merchantUid = {}, amount = {}", customerUid, merchantUid, pay.getAmount());

                    // 결제 요청을 수행
                    IamportResponse<Payment> response = iamportClient.againPayment(againPaymentData);
//                    log.info(response.getMessage());
                    String impUid = response.getResponse().getImpUid();
                    Integer count = pay.getCount();
                    count++;

                    // 결제 성공 여부 확인
                    if (response.getResponse() != null && "paid".equals(response.getResponse().getStatus())) {
                        // 결제가 성공한 경우, 다음 결제일 갱신
                        pay.updateNextPaymentDate(impUid, count);
                        paymentRepository.save(pay);

                        // 메시지에 포함할 데이터 설정
                        String memberEmail = pay.getMemberEmail();
                        BigDecimal fee = pay.getAmount();
                        String name = pay.getName();

                        // 메시지 데이터 객체 생성
                        Map<String, Object> messageData = new HashMap<>();
                        messageData.put("memberEmail", memberEmail);
                        messageData.put("fee", fee);
                        messageData.put("name", name);


                        // 객체를 JSON 문자열로 변환
                        String message = objectMapper.writeValueAsString(messageData);

                        // Kafka로 메시지 전송
                        kafkaTemplate.send("payment-success", message);
                        log.info("결제 성공 메시지를 Kafka로 전송: {}", message);

                        log.info("정기 결제 성공, 다음 결제일 갱신: {}", pay.getSubscriptionEndDate());
                    } else {
                        log.error("정기 결제 실패: {}", response.getMessage());
                        String memberEmail = pay.getMemberEmail();

                        String impUid2 = pay.getImpUid();
                        // 메시지 데이터 객체 생성
                        Map<String, Object> messageData = new HashMap<>();
                        messageData.put("memberEmail", memberEmail);
                        messageData.put("fee", pay.getAmount());
                        messageData.put("impUid", impUid2);

                        // 객체를 JSON 문자열로 변환
                        String message = objectMapper.writeValueAsString(messageData);

                        // Kafka로 메시지 전송
                        kafkaTemplate.send("payment-cancel-fail", message);
                        log.info("결제 성공 메시지를 Kafka로 전송: {}", message);
                        throw new RuntimeException("결제 처리 실패");
                    }
                } catch (Exception e) {
                    log.error("정기 결제 처리 중 오류 발생: ", e);
                }
            }
        }
    }

    // 구독 취소하는 메서드
    public IamportResponse<Payment> cancelSubscription(String impUid) throws Exception {
        String actualImpUid = extractImpUid(impUid);  // impUid를 추출
        Pay pay = paymentRepository.findByImpUid(actualImpUid);  // 결제 정보를 DB에서 조회

        if (pay == null) {
            throw new Exception("해당 결제 정보를 찾을 수 없습니다.");
        }

        // 구독 취소 처리
        pay.cancelSubscription();  // 구독 상태를 취소로 업데이트
        paymentRepository.save(pay);  // 업데이트된 결제 정보를 DB에 저장

        // 구독 취소 후 아임포트에 결제 취소 요청을 보낼 필요가 있을 경우, 여기에 CancelData로 요청
        CancelData cancelData = new CancelData(actualImpUid, true);  // impUid 기반으로 취소 요청 데이터 생성
        IamportResponse<Payment> cancelResponse = iamportClient.cancelPaymentByImpUid(cancelData);  // 아임포트에 취소 요청

        if (cancelResponse.getResponse() != null) {
            log.info("구독 결제 취소 성공: {}", cancelResponse.getResponse());
            String memberEmail = pay.getMemberEmail();
            BigDecimal fee = pay.getAmount();
            String name = pay.getName();

            // 메시지 데이터 객체 생성
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("memberEmail", memberEmail);
            messageData.put("fee", fee);
            messageData.put("name", name);




            // 객체를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(messageData);

            // Kafka로 메시지 전송
            kafkaTemplate.send("payment-cancel", message);
            log.info("결제 취소 메시지를 Kafka로 전송: {}", message);
        } else {
            log.error("구독 결제 취소 실패: {}", cancelResponse.getMessage());
            String memberEmail = pay.getMemberEmail();
            BigDecimal fee = pay.getAmount();
            String name = pay.getName();

            String impUid2 = pay.getImpUid();
            // 메시지 데이터 객체 생성
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("memberEmail", memberEmail);
            messageData.put("fee", fee);
            messageData.put("impUid", impUid2);

            // 객체를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(messageData);

            // Kafka로 메시지 전송
            kafkaTemplate.send("payment-cancel-fail", message);
            log.error("결제 취소 실패 메시지를 Kafka로 전송: {}", message);
            throw new Exception("구독 취소 실패: " + cancelResponse.getMessage());
        }
        return cancelResponse;  // 취소 응답 반환
    }
}
