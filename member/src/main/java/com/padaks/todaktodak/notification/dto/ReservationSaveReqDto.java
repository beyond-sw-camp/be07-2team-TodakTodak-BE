package com.padaks.todaktodak.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationSaveReqDto {
    private String memberEmail;
    private Long childId;
    private Long hospitalId;
    private String doctorEmail;
    private String reservationType;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private boolean untact;
    private String medicalItem;
    private String status;
    private String field;
    private String message;
}
