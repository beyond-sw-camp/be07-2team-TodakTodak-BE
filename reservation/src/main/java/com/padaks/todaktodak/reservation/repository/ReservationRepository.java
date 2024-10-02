package com.padaks.todaktodak.reservation.repository;

import com.padaks.todaktodak.reservation.domain.Reservation;
import com.padaks.todaktodak.reservation.domain.ReserveType;
import com.padaks.todaktodak.reservation.domain.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByMemberEmail(Pageable pageable, String email);
    Page<Reservation> findByMemberEmailAndReservationType(Pageable pageable, String email, ReserveType reserveType);
    Page<Reservation> findByDoctorEmail(Pageable pageable, String email);
    Page<Reservation> findByStatus(Pageable pageable, Status status);

    Optional<Reservation> findByMemberEmailAndReservationType(String email, ReserveType reserveType);
    Optional<Reservation> findByDoctorEmailAndReservationDateAndReservationTime
            (String doctorEmail, LocalDate reservationDate, LocalTime reservationTime);

    Optional<Reservation> findById(Long id);

    Optional<Reservation> findByIdAndStatus(Long id, Status status);

    List<Reservation> findAllByReservationDateAndReservationTime(LocalDate localDate, LocalTime localTime);
}
