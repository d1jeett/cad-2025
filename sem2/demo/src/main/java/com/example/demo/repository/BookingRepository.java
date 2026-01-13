package com.example.demo.repository;

import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import com.example.demo.model.Room;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // Существующие методы
    List<Booking> findByUser(User user);
    List<Booking> findByRoomId(Long roomId);
    List<Booking> findByStatus(BookingStatus status);
    
    // Новые методы для поиска по email
    List<Booking> findByGuestEmail(String guestEmail);
    
    // Метод для проверки доступности комнаты
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
           "FROM Booking b " +
           "WHERE b.room.id = :roomId " +
           "AND b.status IN ('APPROVED', 'PENDING') " +
           "AND (:checkIn < b.checkOutDate) " +
           "AND (:checkOut > b.checkInDate)")
    boolean isRoomBookedForDates(@Param("roomId") Long roomId,
                                 @Param("checkIn") LocalDate checkIn,
                                 @Param("checkOut") LocalDate checkOut);
    
    // Дополнительный метод для поиска по email пользователя
    @Query("SELECT b FROM Booking b WHERE b.user.email = :email")
    List<Booking> findByUserEmail(@Param("email") String email);
    
    // Универсальный поиск по любому email
    @Query("SELECT b FROM Booking b WHERE b.guestEmail = :email OR b.user.email = :email")
    List<Booking> findByAnyEmail(@Param("email") String email);
    
    // Поиск по диапазону дат
    @Query("SELECT b FROM Booking b WHERE b.checkInDate BETWEEN :startDate AND :endDate OR b.checkOutDate BETWEEN :startDate AND :endDate")
    List<Booking> findBookingsBetweenDates(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}