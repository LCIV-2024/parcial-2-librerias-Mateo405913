package com.example.libreria.repository;

import com.example.libreria.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // TOD: Implementar los métodos de la reserva

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByStatus(Reservation.ReservationStatus reservationStatus);

    @Query("SELECT r FROM Reservation r " +
            "WHERE r.expectedReturnDate < CURRENT_DATE " +
            "AND r.status <> com.example.libreria.model.Reservation.ReservationStatus.RETURNED")
    List<Reservation> findOverdueReservations();

}

