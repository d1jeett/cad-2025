package com.example.demo.repository;

import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUser(User user);
    
    List<Booking> findByStatus(BookingStatus status);
    
    List<Booking> findByRoomId(Long roomId);
    
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND " +
           "b.createdAt BETWEEN :startDate AND :endDate")
    List<Booking> findBookingsByStatusAndDateRange(
            @Param("status") BookingStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    // Проверка пересечения дат для комнаты
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.status = 'APPROVED' AND " +
           "(:checkIn < b.checkOutDate AND :checkOut > b.checkInDate)")
    Boolean isRoomBookedForDates(@Param("roomId") Long roomId,
                                 @Param("checkIn") LocalDate checkIn,
                                 @Param("checkOut") LocalDate checkOut);
    
    // Количество активных бронирований пользователя
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND " +
           "b.status IN ('PENDING', 'APPROVED')")
    Long countActiveBookingsByUser(@Param("userId") Long userId);
    
    // Поиск бронирований по email гостя
    List<Booking> findByGuestEmail(String guestEmail);
    
    // Поиск бронирований по периоду дат
    @Query("SELECT b FROM Booking b WHERE " +
           "(:checkIn BETWEEN b.checkInDate AND b.checkOutDate OR " +
           ":checkOut BETWEEN b.checkInDate AND b.checkOutDate OR " +
           "b.checkInDate BETWEEN :checkIn AND :checkOut)")
    List<Booking> findBookingsForDates(@Param("checkIn") LocalDate checkIn,
                                      @Param("checkOut") LocalDate checkOut);
}