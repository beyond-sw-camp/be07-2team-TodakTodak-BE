package com.padaks.todaktodak.reservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.padaks.todaktodak.chatroom.service.UntactChatRoomService;
import com.padaks.todaktodak.common.dto.DtoMapper;
import com.padaks.todaktodak.common.dto.MemberFeignDto;
import com.padaks.todaktodak.common.exception.BaseException;
import com.padaks.todaktodak.common.feign.MemberFeignClient;
import com.padaks.todaktodak.hospital.domain.Hospital;
import com.padaks.todaktodak.hospital.repository.HospitalRepository;
import com.padaks.todaktodak.reservation.domain.Reservation;
import com.padaks.todaktodak.reservation.dto.*;
import com.padaks.todaktodak.reservation.domain.ReservationHistory;
import com.padaks.todaktodak.reservation.domain.Status;
import com.padaks.todaktodak.reservation.repository.ReservationHistoryRepository;
import com.padaks.todaktodak.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.padaks.todaktodak.common.exception.exceptionType.HospitalExceptionType.*;
import static com.padaks.todaktodak.common.exception.exceptionType.ReservationExceptionType.*;
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationHistoryRepository reservationHistoryRepository;
    private final DtoMapper dtoMapper;
    @Qualifier("1")
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final HospitalRepository hospitalRepository;

    private final MemberFeign memberFeign;
    private final UntactChatRoomService untactChatRoomService;

//    진료 스케줄 예약 기능
    public void scheduleReservation(ReservationSaveReqDto dto){
        log.info("ReservationService[scheduleReservation] : 스케줄 예약 요청 처리 시작");
        List<LocalTime> timeSlots = ReservationTimeSlot.timeSlots();

        Hospital hospital = hospitalRepository.findById(dto.getHospitalId())
                        .orElseThrow(() -> new BaseException(HOSPITAL_NOT_FOUND));

        LocalTime selectedTime = dto.getReservationTime();

        if(timeSlots.contains(selectedTime)){
            int partition = timeSlots.indexOf(selectedTime);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModules(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            String message;
            try {
                message = objectMapper.writeValueAsString(dto);
                reservationRepository.findByDoctorEmailAndReservationDateAndReservationTime
                                (dto.getDoctorEmail(), dto.getReservationDate(), dto.getReservationTime())
                        .ifPresent(reservation -> {
                            throw new BaseException(RESERVATION_DUPLICATE);
                        });
                kafkaTemplate.send("reservationSchedule", partition, dto.getDoctorEmail() ,message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

//    당일 예약 기능
    public void immediateReservation(ReservationSaveReqDto dto){
        log.info("ReservationService[immediateReservation] : 예약 요청 처리 시작");
        Hospital hospital = hospitalRepository.findById(dto.getHospitalId())
                .orElseThrow(() -> new BaseException(HOSPITAL_NOT_FOUND));

        MemberResDto memberResDto = memberFeign.getMember(dto.getMemberEmail());
        DoctorResDto doctorResDto = memberFeign.getDoctor(dto.getDoctorEmail());

        int partition = hospital.getId().intValue();
        String doctorKey = dto.getDoctorEmail();
        kafkaTemplate.send("reservationImmediate",partition,doctorKey , dto)
                .addCallback(
                        success -> log.info("Sent message to partition: {}",
                                Objects.requireNonNull(success).getRecordMetadata().partition()),
                        failure -> log.error("Failed to send message", failure)
                );

        log.info("ReservationService[immediateReservation] : Kafka 메시지 전송 완료");
    }

//    예약 취소 기능
    public void cancelledReservation(Long id){
        log.info("ReservationService[cancelledReservation] : 시작");
//        예약의 id 로 찾고 만약 예약이 없을경우 RESERVATION_NOT_FOUND 예외를 발생 -> BaseException 에 정의
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BaseException(RESERVATION_NOT_FOUND));
//        hard delete 로 DB 상에서 완전히 지워버림
        reservationRepository.delete(reservation);
//        reservationHistory 테이블에 저장하기 위한 코드
        ReservationHistory reservationHistory = dtoMapper.toReservationHistory(reservation, 1L);
        reservationHistory.setStatus(Status.Cancelled);
//        reservationHistory 테이블에 저장.
        reservationHistoryRepository.save(reservationHistory);

        //        Redis의 예약 찾기
        String key = reservationHistory.getHospitalId() + ":" + reservationHistory.getDoctorEmail();
        RedisDto redisDto = dtoMapper.toRedisDto(reservation);
//        list 에서 해당 예약을 삭제
        redisTemplate.opsForZSet().remove(key, redisDto);
    }
    
//    예약 조회 기능
    public List<?> checkListReservation(CheckListReservationResDto resDto, Pageable pageable){
//        feign 으로 연결 되면 여기에 email 로 해당 user 찾는 로직이 들어갈 예정
        MemberResDto member = memberFeign.getMember(resDto.getEmail());
//        여기서 페이징 처리할 예정 -> 페이징 처리하면서 예약
//        여기서 미리 예약 , 당일 예약 분기처리도 해줄 예정
        Page<Reservation> reservationPage;
        if(resDto.getType().toString().equals("All")){
            reservationPage = reservationRepository.findByMemberEmail(pageable, resDto.getEmail());
        }
        else{
            reservationPage = reservationRepository
                    .findByMemberEmailAndReservationType(
                            pageable,
                            resDto.getEmail(),
                            dtoMapper.resTypeToReserveType(resDto.getType()));
        }


        List<CheckListReservationReqDto> dto = reservationPage.stream()
                .map(dtoMapper::toListReservation)
                .collect(Collectors.toList());

        return dto;
    }

//    대기열 순위 보기
    public Long rankReservationQueue(Long id){
        Reservation reservation = reservationRepository.findByIdAndStatus(id, Status.Confirmed)
                .orElseThrow(() -> new BaseException(RESERVATION_NOT_FOUND));
        RedisDto redisDto = dtoMapper.toRedisDto(reservation);
        String key = reservation.getHospital().getId() + ":" + reservation.getDoctorEmail();
        return redisTemplate.opsForZSet().rank(key, redisDto);
    }

//    스케줄 예약 노쇼 스케줄 동작 구현
    public List<String> reservationNoShowSchedule(){
        log.info("예약 노쇼 스케줄 동작");
        LocalDate localDate = LocalDate.now();
        LocalTime localTime = LocalTime.now().withNano(0).withSecond(0);

        List<Reservation> reservation =
                reservationRepository.findAllByReservationDateAndReservationTime(localDate,localTime);
        List<String> member = new ArrayList<>();

        for(Reservation res : reservation){
            if(!res.getStatus().equals(Status.Completed)) {
                res.updateStatus(Status.Noshow);
                member.add(res.getMemberEmail());
            }
        }
        log.info("예약 노쇼 스케줄 종료");
        return member;
    }
}
