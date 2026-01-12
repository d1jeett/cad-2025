package com.example.demo.repository;

import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal; // Добавить импорт
import java.time.LocalDate;
import java.util.List;
import java.util.Optional; // Добавить импорт

public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUser(User user);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByRoomId(Long roomId);
    List<Booking> findByGuestEmail(String guestEmail);
    
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND " +
           "b.createdAt BETWEEN :startDate AND :endDate")
    List<Booking> findBookingsByStatusAndDateRange(
            @Param("status") BookingStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    // Проверка пересечения дат для комнаты (только одобренные бронирования)
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.status = 'APPROVED' AND " +
           "(:checkIn < b.checkOutDate AND :checkOut > b.checkInDate)")
    boolean isRoomBookedForDates(@Param("roomId") Long roomId,
                                 @Param("checkIn") LocalDate checkIn,
                                 @Param("checkOut") LocalDate checkOut);
    
    // Статистика бронирований по пользователю
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND " +
           "b.status IN ('PENDING', 'APPROVED')")
    Long countActiveBookingsByUser(@Param("userId") Long userId);
    
    // Бронирования за период
    @Query("SELECT b FROM Booking b WHERE " +
           "(:checkIn BETWEEN b.checkInDate AND b.checkOutDate OR " +
           ":checkOut BETWEEN b.checkInDate AND b.checkOutDate OR " +
           "b.checkInDate BETWEEN :checkIn AND :checkOut)")
    List<Booking> findBookingsForDates(@Param("checkIn") LocalDate checkIn,
                                      @Param("checkOut") LocalDate checkOut);
    
    // Поиск бронирований по диапазону дат и статусу
    @Query("SELECT b FROM Booking b WHERE " +
           "b.checkInDate >= :startDate AND b.checkOutDate <= :endDate AND " +
           "(:status IS NULL OR b.status = :status)")
    List<Booking> findBookingsByDateRangeAndStatus(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") BookingStatus status);
    
    // Статистика доходов
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE " +
           "b.status = 'APPROVED' AND b.checkInDate BETWEEN :startDate AND :endDate")
    BigDecimal getRevenueForPeriod(@Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);
    
    // Количество бронирований по дням
    @Query("SELECT b.checkInDate, COUNT(b) FROM Booking b WHERE " +
           "b.checkInDate BETWEEN :startDate AND :endDate AND b.status = 'APPROVED' " +
           "GROUP BY b.checkInDate ORDER BY b.checkInDate")
    List<Object[]> getBookingCountByDay(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
    
    // Новый метод для проверки доступности
    @Query("SELECT COUNT(b) = 0 FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.status IN ('APPROVED', 'PENDING') " +
           "AND (:checkIn < b.checkOutDate AND :checkOut > b.checkInDate)")
    boolean isRoomAvailable(@Param("roomId") Long roomId,
                          @Param("checkIn") LocalDate checkIn,
                          @Param("checkOut") LocalDate checkOut);
}