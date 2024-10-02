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

    @Autowired
    public ReservationKafkaConsumer(@Qualifier("1") RedisTemplate<String, Object> redisTemplate,
                                    @Qualifier("2") RedisTemplate<String, Object> redisScheduledTemplate, DtoMapper dtoMapper, ReservationRepository reservationRepository, MemberFeign memberFeign, ObjectMapper objectMapper, HospitalRepository hospitalRepository) {
        this.redisTemplate = redisTemplate;
        this.redisScheduleTemplate = redisScheduledTemplate;
        this.dtoMapper = dtoMapper;
        this.reservationRepository = reservationRepository;
        this.memberFeign = memberFeign;
//        Java 8 의 LocalTime, LocalDate 처리를 위한 TimeModule
        objectMapper.registerModules(new JavaTimeModule());
//        파라미터 인수의 각 매채로 매핑 하겠다. - enum
        objectMapper.registerModules(new ParameterNamesModule());
//        의도하지 않은 파라미터는 무시하겠다.
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper = objectMapper;
        this.hospitalRepository = hospitalRepository;
    }

    @KafkaListener(topics = "reservationImmediate", groupId = "Schedule_id", containerFactory = "ppKafkaListenerContainerFactory")
    public void immediateReservation(String message, Acknowledgment acknowledgment) {
        log.info("ReservationConsumer[immediateReservation] : Kafka 메시지 수신");

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
            acknowledgment.acknowledge();
            log.info("KafkaListener[handleReservation] : 예약 대기열 처리 완료");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "reservationSchedule", groupId = "Schedule_id", containerFactory = "ppKafkaListenerContainerFactory")
    public void scheduledReservation(String message, Acknowledgment acknowledgment) {
        log.info("ReservationConsumer[consumerReservation] : Kafka 메시지 수신");

        // 메시지 검증
        if (message == null || message.isEmpty()) {
            log.error("Received empty or null message");
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            return; // 또는 예외 처리
        }

        // 메시지 정제
        if (message.startsWith("\"") && message.endsWith("\"")) {
            message = message.substring(1, message.length() - 1).replace("\"", "\"");
            message = message.replace("\\", "");
        }

        try {
            // JSON 변환
            ReservationSaveReqDto dto = objectMapper.readValue(message, ReservationSaveReqDto.class);

            // 락 키 생성
            String lockKey = dto.getDoctorEmail() + ":" + dto.getReservationDate() + ":" + dto.getReservationTime();
            Boolean isLockState;

            // Redis 템플릿 검증
            if (redisScheduleTemplate != null) {
                isLockState = redisScheduleTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 2, TimeUnit.MINUTES);
            } else {
                log.error("Redis template is null");
                throw new BaseException(REDIS_ERROR);
            }

            // 락 상태 확인
            if (Boolean.TRUE.equals(isLockState)) {
                try {
                    // 중복 예약 체크
                    reservationRepository.findByDoctorEmailAndReservationDateAndReservationTime(
                                    dto.getDoctorEmail(), dto.getReservationDate(), dto.getReservationTime())
                            .ifPresent(reservation -> {
                                throw new BaseException(RESERVATION_DUPLICATE);
                            });

                    // 병원 정보 조회
                    Hospital hospital = hospitalRepository.findById(dto.getHospitalId())
                            .orElseThrow(() -> new BaseException(HOSPITAL_NOT_FOUND));

                    // 예약 생성 및 저장
                    Reservation reservation = dtoMapper.toReservation(dto, hospital);
                    Reservation savedReservation = reservationRepository.save(reservation);
                    sendReservationNotification(savedReservation);
                } finally {
                    // 락 해제
                    redisScheduleTemplate.delete(lockKey);
                    log.info("ReservationConsumer[consumerReservation] : 락 해제 완료");
                }
            } else {
                log.info("ReservationConsumer[consumerReservation] : 락을 얻지 못함, 예약 처리 실패");
                throw new BaseException(LOCK_OCCUPANCY);
            }
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 처리중 JSON 처리 오류 발생: {}", e.getMessage(), e);
        } catch (BaseException e) {
            log.error("Kafka 메시지 처리중 비즈니스 예외 발생: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Kafka 메시지 처리중 알 수 없는 오류 발생: {}", e.getMessage(), e);
        } finally {
            // Acknowledge 처리
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
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
