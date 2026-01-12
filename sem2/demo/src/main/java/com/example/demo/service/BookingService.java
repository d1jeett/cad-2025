package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    
    public BookingService(BookingRepository bookingRepository, 
                         RoomRepository roomRepository,
                         UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Создание бронирования
     */
    public Booking createBooking(Long roomId, Long userId, LocalDate checkIn,
                               LocalDate checkOut, String guestName,
                               String guestEmail, String specialRequests) {
        
        // Проверяем даты
        if (checkIn.isBefore(LocalDate.now().plusDays(1))) {
            throw new IllegalArgumentException("Дата заезда должна быть не ранее завтра");
        }
        if (checkOut.isBefore(checkIn.plusDays(1))) {
            throw new IllegalArgumentException("Минимальная продолжительность бронирования - 1 ночь");
        }
        if (checkOut.isAfter(checkIn.plusDays(30))) {
            throw new IllegalArgumentException("Максимальная продолжительность бронирования - 30 дней");
        }
        
        // Проверяем доступность комнаты
        if (!isRoomAvailableForDates(roomId, checkIn, checkOut)) {
            throw new IllegalArgumentException("Комната не доступна на выбранные даты");
        }
        
        // Получаем комнату и пользователя
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Комната не найдена"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        // Создаем бронирование
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setUser(user);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setGuestName(guestName);
        booking.setGuestEmail(guestEmail);
        booking.setSpecialRequests(specialRequests);
        booking.setStatus(BookingStatus.PENDING);
        
        // Рассчитываем цену
        BigDecimal totalPrice = calculateTotalPrice(roomId, checkIn, checkOut);
        booking.setTotalPrice(totalPrice);
        
        return bookingRepository.save(booking);
    }
    
    /**
     * Получить бронирования пользователя
     */
    public List<Booking> getUserBookings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        return bookingRepository.findByUser(user);
    }
    
    /**
     * Получить бронирование по ID
     */
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Бронирование не найдено"));
    }
    
    /**
     * Получить ожидающие бронирования
     */
    public List<Booking> getPendingBookings() {
        return bookingRepository.findByStatus(BookingStatus.PENDING);
    }
    
    /**
     * Одобрить бронирование
     */
    public void approveBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Можно одобрять только ожидающие бронирования");
        }
        
        // Проверяем, что комната все еще доступна
        if (!isRoomAvailableForDates(booking.getRoom().getId(),
                                   booking.getCheckInDate(),
                                   booking.getCheckOutDate())) {
            throw new IllegalArgumentException("Комната больше не доступна на эти даты");
        }
        
        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);
    }
    
    /**
     * Отклонить бронирование
     */
    public void rejectBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Можно отклонять только ожидающие бронирования");
        }
        
        booking.setStatus(BookingStatus.REJECTED);
        bookingRepository.save(booking);
    }
    
    /**
     * Отменить бронирование пользователем
     */
    public void cancelBooking(Long bookingId, Long userId) {
        Booking booking = getBookingById(bookingId);
        
        // Проверяем, что пользователь отменяет свое бронирование
        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Вы можете отменять только свои бронирования");
        }
        
        // Проверяем, что бронирование можно отменить
        if (!booking.canBeCancelled()) {
            throw new IllegalArgumentException("Бронирование нельзя отменить");
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }
    
    /**
     * Получить все бронирования
     */
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
    
    /**
     * Получить бронирования для комнаты
     */
    public List<Booking> getBookingsByRoom(Long roomId) {
        return bookingRepository.findByRoomId(roomId);
    }
    
    /**
     * Проверить доступность комнаты на даты
     */
    public boolean isRoomAvailableForDates(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isBefore(LocalDate.now())) {
            return false;
        }
        if (!checkOut.isAfter(checkIn)) {
            return false;
        }
        
        // Проверяем доступность комнаты
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty() || !roomOpt.get().isAvailable()) {
            return false;
        }
        
        // Проверяем, нет ли пересечений с другими бронированиями
        return !bookingRepository.isRoomBookedForDates(roomId, checkIn, checkOut);
    }
    
    /**
     * Получить доступные комнаты на даты
     */
    public List<Room> getAvailableRoomsForDates(LocalDate checkIn, LocalDate checkOut) {
        List<Room> allRooms = roomRepository.findByAvailableTrue();
        
        return allRooms.stream()
                .filter(room -> isRoomAvailableForDates(room.getId(), checkIn, checkOut))
                .toList();
    }
    
    /**
     * Расчет стоимости бронирования
     */
    public BigDecimal calculateTotalPrice(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        Room room = getRoomById(roomId);
        long days = ChronoUnit.DAYS.between(checkIn, checkOut);
        return room.getPrice().multiply(BigDecimal.valueOf(days));
    }
    
    /**
     * Получить комнату по ID
     */
    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Комната не найдена"));
    }
}