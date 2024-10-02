package com.padaks.todaktodak.reservation.controller;

import com.padaks.todaktodak.common.exception.BaseException;
import com.padaks.todaktodak.reservation.domain.Reservation;
import com.padaks.todaktodak.reservation.dto.CheckListReservationResDto;
import com.padaks.todaktodak.reservation.dto.ReservationSaveReqDto;
import com.padaks.todaktodak.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservation")
@Slf4j
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/scheduled")
    public ResponseEntity<?> treatScheduledReservation(@RequestBody ReservationSaveReqDto dto){
        try{
            reservationService.scheduleReservation(dto);
            return new ResponseEntity<>("스케쥴 예약 완료", HttpStatus.OK);
        }catch (BaseException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/immediate")
    public ResponseEntity<?> treatImmediateReservation(@RequestBody ReservationSaveReqDto dto){
        reservationService.immediateReservation(dto);
        return new ResponseEntity<>("예약 완료", HttpStatus.OK);
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<?> cancelledReservation(@PathVariable Long id){
        reservationService.cancelledReservation(id);

        return new ResponseEntity<>("취소 완료", HttpStatus.OK);
    }

//    rest 에서 조회할 때는 @PathVariable 로 email 로 조회하겠음
//    추후에 email 로 해도 되고 id 로 해도 될 듯?
    @GetMapping("/list")
    public ResponseEntity<?> listReservation(@RequestBody CheckListReservationResDto resDto, Pageable pageable){
        List<?> dto = reservationService.checkListReservation(resDto, pageable);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @GetMapping("/rank/{id}")
    public ResponseEntity<?> rankReservationQueue(@PathVariable Long id){
        Long res = reservationService.rankReservationQueue(id);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping("/get/member")
    public List<String> getMember(){
        return reservationService.reservationNoShowSchedule();
    }
}
