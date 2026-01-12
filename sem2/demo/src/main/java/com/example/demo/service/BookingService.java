package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public BookingService(BookingRepository bookingRepository, 
                         RoomRepository roomRepository,
                         UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Создание нового бронирования
     */
    public Booking createBooking(Long roomId, Long userId, LocalDate checkIn, 
                                LocalDate checkOut, String guestName, 
                                String guestEmail, String specialRequests) {
        
        // Валидация входных данных
        validateBookingDates(checkIn, checkOut);
        
        // Получение комнаты и пользователя
        Room room = getRoomById(roomId);
        User user = getUserById(userId);
        
        // Проверка доступности комнаты
        validateRoomAvailability(roomId, room, checkIn, checkOut);
        
        // Проверка пересечения с существующими бронированиями
        checkForDateConflicts(roomId, checkIn, checkOut);
        
        // Создание бронирования
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setUser(user);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setGuestName(guestName);
        booking.setGuestEmail(guestEmail);
        booking.setSpecialRequests(specialRequests);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDate.now());
        
        // Автоматическое отклонение пересекающихся бронирований
        autoRejectConflictingPendingBookings(roomId, checkIn, checkOut);
        
        return bookingRepository.save(booking);
    }
    
    /**
     * Валидация дат бронирования
     */
    private void validateBookingDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Даты заезда и выезда обязательны");
        }
        
        LocalDate today = LocalDate.now();
        
        // Проверка на прошлые даты
        if (checkIn.isBefore(today)) {
            throw new IllegalArgumentException("Дата заезда не может быть в прошлом");
        }
        
        // Проверка минимальной длительности
        if (checkOut.isBefore(checkIn.plusDays(1))) {
            throw new IllegalArgumentException("Минимальная длительность проживания - 1 ночь");
        }
        
        // Проверка максимальной длительности (например, 30 дней)
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > 30) {
            throw new IllegalArgumentException("Максимальная длительность проживания - 30 дней");
        }
    }
    
    /**
     * Получение комнаты по ID
     */
    private Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Комната не найдена"));
    }
    
    /**
     * Получение пользователя по ID
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }
    
    /**
     * Проверка доступности комнаты
     */
    private void validateRoomAvailability(Long roomId, Room room, LocalDate checkIn, LocalDate checkOut) {
        // Проверка общей доступности комнаты
        if (!room.isAvailable()) {
            throw new IllegalArgumentException("Комната временно недоступна для бронирования");
        }
        
        // Проверка на уже одобренные бронирования
        if (isRoomBookedForApprovedDates(roomId, checkIn, checkOut)) {
            throw new IllegalArgumentException("Комната уже забронирована на выбранные даты");
        }
    }
    
    /**
     * Проверка пересечения дат с существующими бронированиями
     */
    private void checkForDateConflicts(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        List<Booking> existingBookings = bookingRepository.findByRoomId(roomId);
        
        for (Booking existing : existingBookings) {
            // Проверяем только активные бронирования
            if (existing.getStatus() == BookingStatus.APPROVED || 
                existing.getStatus() == BookingStatus.PENDING) {
                
                if (datesOverlap(checkIn, checkOut, 
                    existing.getCheckInDate(), existing.getCheckOutDate())) {
                    throw new IllegalArgumentException("Комната занята на выбранные даты");
                }
            }
        }
    }
    
    /**
     * Проверка пересечения двух периодов дат
     */
    private boolean datesOverlap(LocalDate start1, LocalDate end1, 
                                LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
    
    /**
     * Проверка занятости комнаты для одобренных бронирований
     */
    public boolean isRoomBookedForApprovedDates(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        List<Booking> roomBookings = bookingRepository.findByRoomId(roomId);
        
        return roomBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.APPROVED)
            .anyMatch(b -> datesOverlap(checkIn, checkOut, 
                                      b.getCheckInDate(), b.getCheckOutDate()));
    }
    
    /**
     * Автоматическое отклонение конфликтующих ожидающих бронирований
     */
    private void autoRejectConflictingPendingBookings(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        List<Booking> pendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING);
        
        pendingBookings.stream()
            .filter(b -> b.getRoom().getId().equals(roomId))
            .filter(b -> datesOverlap(checkIn, checkOut, 
                                    b.getCheckInDate(), b.getCheckOutDate()))
            .forEach(b -> {
                b.setStatus(BookingStatus.REJECTED);
                b.setSpecialRequests((b.getSpecialRequests() != null ? b.getSpecialRequests() : "") + 
                    " [Автоматически отклонено: комната занята на выбранные даты]");
                bookingRepository.save(b);
            });
    }
    
    /**
     * Одобрение бронирования
     */
    public Booking approveBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Бронирование не найдено"));
        
        // Проверяем, что бронирование в статусе PENDING
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Можно одобрять только ожидающие бронирования");
        }
        
        // Проверяем доступность комнаты на даты
        if (isRoomBookedForApprovedDates(booking.getRoom().getId(), 
            booking.getCheckInDate(), booking.getCheckOutDate())) {
            throw new IllegalArgumentException("Комната уже занята на эти даты другим бронированием");
        }
        
        // Автоматически отклоняем пересекающиеся бронирования
        autoRejectConflictingPendingBookings(
            booking.getRoom().getId(),
            booking.getCheckInDate(),
            booking.getCheckOutDate()
        );
        
        booking.setStatus(BookingStatus.APPROVED);
        return bookingRepository.save(booking);
    }
    
    /**
     * Отклонение бронирования
     */
    public Booking rejectBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Бронирование не найдено"));
        
        // Проверяем, что бронирование активное
        if (booking.getStatus() != BookingStatus.PENDING && 
            booking.getStatus() != BookingStatus.APPROVED) {
            throw new IllegalArgumentException("Можно отклонить только активные бронирования");
        }
        
        booking.setStatus(BookingStatus.REJECTED);
        return bookingRepository.save(booking);
    }
    
    /**
     * Отмена бронирования пользователем
     */
    public Booking cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Бронирование не найдено"));
        
        // Проверяем, принадлежит ли бронь пользователю
        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Вы можете отменять только свои бронирования");
        }
        
        // Проверяем, можно ли отменить бронирование
        if (!canBookingBeCancelled(booking)) {
            throw new IllegalArgumentException("Это бронирование не может быть отменено");
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }
    
    /**
     * Проверка возможности отмены бронирования
     */
    private boolean canBookingBeCancelled(Booking booking) {
        // Можно отменить только активные бронирования
        if (booking.getStatus() != BookingStatus.PENDING && 
            booking.getStatus() != BookingStatus.APPROVED) {
            return false;
        }
        
        // Нельзя отменить менее чем за 24 часа до заезда
        LocalDate today = LocalDate.now();
        return booking.getCheckInDate().isAfter(today.plusDays(1));
    }
    
    /**
     * Получение бронирований пользователя
     */
    public List<Booking> getUserBookings(Long userId) {
        User user = getUserById(userId);
        return bookingRepository.findByUser(user);
    }
    
    /**
     * Получение ожидающих бронирований
     */
    public List<Booking> getPendingBookings() {
        return bookingRepository.findByStatus(BookingStatus.PENDING);
    }
    
    /**
     * Получение одобренных бронирований
     */
    public List<Booking> getApprovedBookings() {
        return bookingRepository.findByStatus(BookingStatus.APPROVED);
    }
    
    /**
     * Получение всех бронирований
     */
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
    
    /**
     * Получение бронирования по ID
     */
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Бронирование не найдено"));
    }
    
    /**
     * Получение доступных комнат на указанные даты
     */
    public List<Room> getAvailableRoomsForDates(LocalDate checkIn, LocalDate checkOut) {
        validateBookingDates(checkIn, checkOut);
        
        // Получаем все доступные комнаты
        List<Room> allAvailableRooms = roomRepository.findByAvailableTrue();
        
        // Фильтруем те, что не заняты на указанные даты
        return allAvailableRooms.stream()
            .filter(room -> !isRoomBookedForApprovedDates(room.getId(), checkIn, checkOut))
            .collect(Collectors.toList());
    }
    
    /**
     * Проверка доступности комнаты на даты
     */
    public boolean isRoomAvailableForDates(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty() || !roomOpt.get().isAvailable()) {
            return false;
        }
        
        return !isRoomBookedForApprovedDates(roomId, checkIn, checkOut);
    }
    
    /**
     * Расчет стоимости бронирования
     */
    public Double calculateTotalPrice(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        Room room = getRoomById(roomId);
        long days = ChronoUnit.DAYS.between(checkIn, checkOut);
        return room.getPrice() * days;
    }
    
    /**
     * Получение бронирований по комнате
     */
    public List<Booking> getBookingsByRoom(Long roomId) {
        return bookingRepository.findByRoomId(roomId);
    }
    
    /**
     * Получение бронирований по статусу и диапазону дат
     */
    public List<Booking> getBookingsByStatusAndDateRange(BookingStatus status, 
                                                         LocalDate startDate, 
                                                         LocalDate endDate) {
        return bookingRepository.findBookingsByStatusAndDateRange(status, startDate, endDate);
    }
    
    /**
     * Получение статистики бронирований
     */
    public BookingStats getBookingStats() {
        List<Booking> allBookings = getAllBookings();
        
        BookingStats stats = new BookingStats();
        stats.setTotalBookings(allBookings.size());
        stats.setPendingBookings((int) allBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.PENDING).count());
        stats.setApprovedBookings((int) allBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.APPROVED).count());
        stats.setRejectedBookings((int) allBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.REJECTED).count());
        stats.setCancelledBookings((int) allBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CANCELLED).count());
        
        // Расчет общей выручки
        double totalRevenue = allBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.APPROVED)
            .mapToDouble(Booking::getTotalPrice)
            .sum();
        stats.setTotalRevenue(totalRevenue);
        
        return stats;
    }
    
    /**
     * DTO для статистики
     */
    public static class BookingStats {
        private int totalBookings;
        private int pendingBookings;
        private int approvedBookings;
        private int rejectedBookings;
        private int cancelledBookings;
        private double totalRevenue;
        
        // Геттеры и сеттеры
        public int getTotalBookings() { return totalBookings; }
        public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }
        
        public int getPendingBookings() { return pendingBookings; }
        public void setPendingBookings(int pendingBookings) { this.pendingBookings = pendingBookings; }
        
        public int getApprovedBookings() { return approvedBookings; }
        public void setApprovedBookings(int approvedBookings) { this.approvedBookings = approvedBookings; }
        
        public int getRejectedBookings() { return rejectedBookings; }
        public void setRejectedBookings(int rejectedBookings) { this.rejectedBookings = rejectedBookings; }
        
        public int getCancelledBookings() { return cancelledBookings; }
        public void setCancelledBookings(int cancelledBookings) { this.cancelledBookings = cancelledBookings; }
        
        public double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
    }
    
    /**
     * Поиск бронирований по email гостя
     */
    public List<Booking> findBookingsByGuestEmail(String email) {
        return bookingRepository.findByGuestEmail(email);
    }
    
    /**
     * Поиск бронирований за период
     */
    public List<Booking> findBookingsForDates(LocalDate checkIn, LocalDate checkOut) {
        return bookingRepository.findBookingsForDates(checkIn, checkOut);
    }
    
    /**
     * Обновление бронирования (только для модератора/админа)
     */
    public Booking updateBooking(Long bookingId, LocalDate newCheckIn, LocalDate newCheckOut, 
                                String guestName, String guestEmail, String specialRequests) {
        Booking booking = getBookingById(bookingId);
        
        // Проверяем, что бронирование можно редактировать
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Можно редактировать только ожидающие бронирования");
        }
        
        // Если изменились даты, проверяем доступность
        if (!booking.getCheckInDate().equals(newCheckIn) || 
            !booking.getCheckOutDate().equals(newCheckOut)) {
            
            validateBookingDates(newCheckIn, newCheckOut);
            validateRoomAvailability(booking.getRoom().getId(), booking.getRoom(), newCheckIn, newCheckOut);
            checkForDateConflicts(booking.getRoom().getId(), newCheckIn, newCheckOut);
        }
        
        booking.setCheckInDate(newCheckIn);
        booking.setCheckOutDate(newCheckOut);
        booking.setGuestName(guestName);
        booking.setGuestEmail(guestEmail);
        booking.setSpecialRequests(specialRequests);
        
        return bookingRepository.save(booking);
    }
}