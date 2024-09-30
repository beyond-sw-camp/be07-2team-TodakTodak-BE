package com.padaks.todaktodak.reservation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.padaks.todaktodak.common.dto.DtoMapper;
import com.padaks.todaktodak.common.exception.BaseException;
import com.padaks.todaktodak.hospital.domain.Hospital;
import com.padaks.todaktodak.hospital.repository.HospitalRepository;
import com.padaks.todaktodak.reservation.domain.Reservation;
import com.padaks.todaktodak.reservation.dto.MemberResDto;
import com.padaks.todaktodak.reservation.dto.NotificationReqDto;
import com.padaks.todaktodak.reservation.dto.RedisDto;
import com.padaks.todaktodak.reservation.dto.ReservationSaveReqDto;
import com.padaks.todaktodak.reservation.realtime.RealTimeService;
import com.padaks.todaktodak.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;
import retrofit2.http.Header;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.padaks.todaktodak.common.exception.exceptionType.ReservationExceptionType.*;
import static com.padaks.todaktodak.common.exception.exceptionType.HospitalExceptionType.HOSPITAL_NOT_FOUND;

@Service
@Slf4j
public class ReservationKafkaConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, Object> redisScheduleTemplate;
    private final DtoMapper dtoMapper;
    private final ReservationRepository reservationRepository;
    private final MemberFeign memberFeign;
    private final ObjectMapper objectMapper;
    private final HospitalRepository hospitalRepository;
    private final RealTimeService realTimeService;

    @Autowired
    public ReservationKafkaConsumer(@Qualifier("1") RedisTemplate<String, Object> redisTemplate,
                                    @Qualifier("2") RedisTemplate<String, Object> redisScheduledTemplate, DtoMapper dtoMapper, ReservationRepository reservationRepository, MemberFeign memberFeign, ObjectMapper objectMapper, HospitalRepository hospitalRepository, RealTimeService realTimeService) {
        this.redisTemplate = redisTemplate;
        this.redisScheduleTemplate = redisScheduledTemplate;
        this.dtoMapper = dtoMapper;
        this.reservationRepository = reservationRepository;
        this.memberFeign = memberFeign;
        this.realTimeService = realTimeService;
//        Java 8 의 LocalTime, LocalDate 처리를 위한 TimeModule
        objectMapper.registerModules(new JavaTimeModule());
//        파라미터 인수의 각 매채로 매핑 하겠다. - enum
        objectMapper.registerModules(new ParameterNamesModule());
//        의도하지 않은 파라미터는 무시하겠다.
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper = objectMapper;
        this.hospitalRepository = hospitalRepository;
    }

// 레디스의 동작 여부를 감지
    private boolean isRedisAvailable(){
        try {
            // Redis에 간단한 ping 요청을 보내서 상태를 확인
            return redisTemplate.getConnectionFactory().getConnection().ping().equals("PONG");
        } catch (Exception e) {
            return false; // Redis가 꺼져 있거나 연결이 실패한 경우
        }
    }

    @KafkaListener(topics = "reservationImmediate", groupId = "group_id", containerFactory = "kafkaListenerContainerFactory")
    public void consumerReservation(String message,
                                    @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String hospitalKey,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION_ID) String partition) {
        log.info("ReservationConsumer[consumerReservation] : Kafka 메시지 수신 - 병원 파티션 {}, 의사 {}", hospitalKey, partition);

        try {
            ReservationSaveReqDto dto = objectMapper.readValue(message, ReservationSaveReqDto.class);
            String key = dto.getHospitalId() + ":" + dto.getDoctorEmail();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                Set<Object> set = redisTemplate.opsForZSet().range(key, 0, -1);
                if (set.size() > 30) {
                    throw new BaseException(TOOMANY_RESERVATION);
                }
            }
            String sequenceKey = "sequence" + dto.getHospitalId();
            Long sequence = redisTemplate.opsForValue().increment(sequenceKey, 1);

            Hospital hospital = hospitalRepository.findById(dto.getHospitalId())
                    .orElseThrow(() -> new BaseException(HOSPITAL_NOT_FOUND));
            Reservation reservation = dtoMapper.toReservation(dto, hospital);
            reservationRepository.save(reservation);
            RedisDto redisDto = dtoMapper.toRedisDto(reservation);

            redisTemplate.opsForZSet().add(key, redisDto, sequence);
            realTimeService.update(reservation.getId().toString(),sequence.toString());
            log.info("KafkaListener[handleReservation] : 예약 대기열 처리 완료");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "reservationSchedule", groupId = "Schedule_id", containerFactory = "kafkaListenerContainerFactory")
    public void scheduledReservation(String message,
                                     @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String doctorKey,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION_ID) String partition,
                                     Acknowledgment acknowledgment) {
        log.info("ReservationConsumer[consumerReservation] : Kafka 메시지 수신");
        if (message.startsWith("\"") && message.endsWith("\"")) {
            message = message.substring(1, message.length() -1).replace("\"", "\"");
            message = message.replace("\\", "");
        }
        try {
            ReservationSaveReqDto dto =
                    objectMapper.readValue(message, ReservationSaveReqDto.class);

            String lockKey = dto.getDoctorEmail() + ":"+ dto.getReservationDate()+ ":" + dto.getReservationTime();
//            해당 lock 이 2분동안 유지되도록 설정.
//            2 : 얼마나 유지할 것인가
//            TimeUnit.MINUTES :
//                  MINUTES - 분
//                  HOURS - 시간
//                  DAYS - 일
            Boolean isLockState;
            if (redisScheduleTemplate != null && redisScheduleTemplate.opsForValue() != null) {
                isLockState = redisScheduleTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 2, TimeUnit.MINUTES);
            } else {
                // 로그 출력 또는 예외 처리
                throw new IllegalStateException("redisTemplate or opsForValue is null");
            }
            if(Boolean.TRUE.equals(isLockState)){
                try{
                    //        진료 예약 시 해당 의사 선생님의 예약이 존재할 경우 Exception을 발생 시키기 위한 코드
                    reservationRepository.findByDoctorEmailAndReservationDateAndReservationTime
                                    (dto.getDoctorEmail(), dto.getReservationDate(), dto.getReservationTime())
                            .ifPresent(reservation -> {
                                throw new BaseException(RESERVATION_DUPLICATE);
                            });
                    Hospital hospital = hospitalRepository.findById(dto.getHospitalId())
                            .orElseThrow(() -> new BaseException(HOSPITAL_NOT_FOUND));
                    Reservation reservation = dtoMapper.toReservation(dto, hospital);
                    Reservation savedReservation = reservationRepository.save(reservation);
                    sendReservationNotification(savedReservation);
                }finally {
                    redisScheduleTemplate.delete(lockKey);
                    log.info("ReservationConsumer[consumerReservation] : 락 해제 완료");
                }
            }else{
                log.info("ReservationConsumer[consumerReservation] : 락을 얻지 못함, 예약 처리 실패");
                throw new BaseException(LOCK_OCCUPANCY);
            }
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 처리중 JSON 처리 오류 발생: {}", e.getMessage());
        } catch (BaseException e){
            log.error("Kafka 메시지 처리중 JSON 처리 오류 발생: {}", e.getMessage());
        }
        finally {
            // 예외 여부에 상관없이 메시지는 acknowledge 처리
            acknowledgment.acknowledge();
        }
    }

    public void sendReservationNotification(Reservation reservation) {
        MemberResDto member = memberFeign.getMemberByEmail(reservation.getMemberEmail());
        String content = String.format("환자 %s님이 %s에 %s의 예약을 했습니다.",
                member.getName(),
                reservation.getReservationDate().toString(),
                reservation.getReservationTime().toString()
        );
//        알림 받는 사람 병원 admin Email로 변경해야함
        NotificationReqDto notificationReqDto = NotificationReqDto.builder()
                .memberEmail(reservation.getDoctorEmail())
                .type("RESERVATION_NOTIFICATION")
                .content(content)
                .refId(reservation.getId())
                .build();

        memberFeign.sendReservationNotification(notificationReqDto);
    }
}
