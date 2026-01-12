package com.example.demo.scheduler;

import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import com.example.demo.repository.BookingRepository;
import com.example.demo.service.BookingService;
import com.example.demo.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
public class BookingScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingScheduler.class);
    
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final RoomService roomService;
    
    @Autowired
    public BookingScheduler(BookingRepository bookingRepository,
                           BookingService bookingService,
                           RoomService roomService) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.roomService = roomService;
    }
    
    /**
     * Автоматическая проверка и отклонение конфликтующих бронирований
     * Выполняется каждые 5 минут
     */
    @Scheduled(fixedRate = 300000) // 5 минут = 300000 мс
    @Transactional
    public void checkAndResolveBookingConflicts() {
        logger.info("Начало автоматической проверки конфликтующих бронирований: {}", LocalDateTime.now());
        
        List<Booking> pendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING);
        List<Booking> approvedBookings = bookingRepository.findByStatus(BookingStatus.APPROVED);
        
        int rejectedCount = 0;
        
        for (Booking pending : pendingBookings) {
            boolean hasConflict = false;
            String conflictReason = "";
            
            // Проверяем пересечение с одобренными бронированиями
            for (Booking approved : approvedBookings) {
                if (pending.getRoom().getId().equals(approved.getRoom().getId())) {
                    if (datesOverlap(pending.getCheckInDate(), pending.getCheckOutDate(),
                                     approved.getCheckInDate(), approved.getCheckOutDate())) {
                        hasConflict = true;
                        conflictReason = "Конфликт с одобренным бронированием #" + approved.getId();
                        break;
                    }
                }
            }
            
            // Проверяем пересечение с другими ожидающими бронированиями той же комнаты
            if (!hasConflict) {
                for (Booking otherPending : pendingBookings) {
                    if (!pending.getId().equals(otherPending.getId()) &&
                        pending.getRoom().getId().equals(otherPending.getRoom().getId()) &&
                        otherPending.getCreatedAt().isBefore(pending.getCreatedAt())) {
                        
                        if (datesOverlap(pending.getCheckInDate(), pending.getCheckOutDate(),
                                        otherPending.getCheckInDate(), otherPending.getCheckOutDate())) {
                            hasConflict = true;
                            conflictReason = "Конфликт с более ранним ожидающим бронированием #" + otherPending.getId();
                            break;
                        }
                    }
                }
            }
            
            // Автоматически отклоняем конфликтующие бронирования
            if (hasConflict) {
                try {
                    pending.setStatus(BookingStatus.REJECTED);
                    String currentRequests = pending.getSpecialRequests() != null ? 
                                             pending.getSpecialRequests() + "\n" : "";
                    pending.setSpecialRequests(currentRequests + 
                        "[Авто-отклонение " + LocalDateTime.now() + "]: " + conflictReason);
                    
                    bookingRepository.save(pending);
                    rejectedCount++;
                    
                    logger.info("Автоматически отклонено бронирование #{}: {}", pending.getId(), conflictReason);
                    
                } catch (Exception e) {
                    logger.error("Ошибка при авто-отклонении бронирования #{}: {}", pending.getId(), e.getMessage());
                }
            }
        }
        
        if (rejectedCount > 0) {
            logger.info("Автоматически отклонено {} конфликтующих бронирований", rejectedCount);
        }
    }
    
    /**
     * Автоматическое завершение прошедших бронирований
     * Выполняется каждый день в полночь
     */
    @Scheduled(cron = "0 0 0 * * *") // Каждый день в 00:00
    @Transactional
    public void completePastBookings() {
        logger.info("Начало автоматического завершения прошедших бронирований: {}", LocalDateTime.now());
        
        List<Booking> approvedBookings = bookingRepository.findByStatus(BookingStatus.APPROVED);
        LocalDate today = LocalDate.now();
        
        int completedCount = 0;
        
        for (Booking booking : approvedBookings) {
            // Если дата выезда уже прошла
            if (booking.getCheckOutDate().isBefore(today)) {
                try {
                    booking.setStatus(BookingStatus.COMPLETED);
                    bookingRepository.save(booking);
                    completedCount++;
                    
                    logger.info("Бронирование #{} автоматически завершено (выезд {})", 
                               booking.getId(), booking.getCheckOutDate());
                    
                } catch (Exception e) {
                    logger.error("Ошибка при завершении бронирования #{}: {}", booking.getId(), e.getMessage());
                }
            }
        }
        
        if (completedCount > 0) {
            logger.info("Автоматически завершено {} прошедших бронирований", completedCount);
        }
    }
    
    /**
     * Автоматическая отмена просроченных ожидающих бронирований
     * (более 24 часов без решения)
     * Выполняется каждый час
     */
    @Scheduled(fixedRate = 3600000) // 1 час
    @Transactional
    public void cancelExpiredPendingBookings() {
        logger.info("Начало проверки просроченных ожидающих бронирований: {}", LocalDateTime.now());
        
        List<Booking> pendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING);
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        
        int cancelledCount = 0;
        
        for (Booking booking : pendingBookings) {
            // Если бронирование ожидает более 24 часов
            if (booking.getCreatedAt().atStartOfDay().isBefore(cutoffTime)) {
                try {
                    booking.setStatus(BookingStatus.CANCELLED);
                    String currentRequests = booking.getSpecialRequests() != null ? 
                                            booking.getSpecialRequests() + "\n" : "";
                    booking.setSpecialRequests(currentRequests + 
                        "[Авто-отмена " + LocalDateTime.now() + "]: Бронирование просрочено (более 24 часов без решения)");
                    
                    bookingRepository.save(booking);
                    cancelledCount++;
                    
                    logger.info("Бронирование #{} автоматически отменено (создано {})", 
                               booking.getId(), booking.getCreatedAt());
                    
                } catch (Exception e) {
                    logger.error("Ошибка при отмене просроченного бронирования #{}: {}", booking.getId(), e.getMessage());
                }
            }
        }
        
        if (cancelledCount > 0) {
            logger.info("Автоматически отменено {} просроченных ожидающих бронирований", cancelledCount);
        }
    }
    
    /**
     * Проверка доступности комнат в реальном времени
     * Выполняется каждые 10 минут
     */
    @Scheduled(fixedRate = 600000) // 10 минут
    @Transactional
    public void updateRoomAvailabilityStatus() {
        logger.info("Начало обновления статуса доступности комнат: {}", LocalDateTime.now());
        
        try {
            // Получаем все комнаты
            var rooms = roomService.getAllRooms();
            LocalDate today = LocalDate.now();
            
            int updatedCount = 0;
            
            for (var room : rooms) {
                // Получаем все активные бронирования для комнаты
                List<Booking> roomBookings = bookingRepository.findByRoomId(room.getId());
                
                boolean hasActiveBookings = roomBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.APPROVED || 
                                b.getStatus() == BookingStatus.PENDING)
                    .anyMatch(b -> !b.getCheckOutDate().isBefore(today));
                
                // Обновляем статус доступности
                boolean shouldBeAvailable = !hasActiveBookings && room.isAvailable();
                
                if (room.isAvailable() != shouldBeAvailable) {
                    room.setAvailable(shouldBeAvailable);
                    roomService.saveRoom(room);
                    updatedCount++;
                    
                    logger.info("Обновлен статус доступности комнаты #{}: {} -> {}", 
                               room.getNumber(), !shouldBeAvailable, shouldBeAvailable);
                }
            }
            
            if (updatedCount > 0) {
                logger.info("Обновлен статус доступности для {} комнат", updatedCount);
            }
            
        } catch (Exception e) {
            logger.error("Ошибка при обновлении статуса доступности комнат: {}", e.getMessage());
        }
    }
    
    /**
     * Отправка напоминаний о предстоящих заездах
     * Выполняется каждый день в 9:00
     */
    @Scheduled(cron = "0 0 9 * * *") // Каждый день в 09:00
    public void sendCheckInReminders() {
        logger.info("Начало отправки напоминаний о предстоящих заездах: {}", LocalDateTime.now());
        
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Booking> approvedBookings = bookingRepository.findByStatus(BookingStatus.APPROVED);
        
        int reminderCount = 0;
        
        for (Booking booking : approvedBookings) {
            // Если заезд завтра
            if (booking.getCheckInDate().equals(tomorrow)) {
                try {
                    // Здесь можно добавить отправку email или уведомления
                    logger.info("НАПОМИНАНИЕ: Гость {} заезжает завтра в комнату #{}, бронирование #{}", 
                               booking.getGuestName(), booking.getRoom().getNumber(), booking.getId());
                    
                    reminderCount++;
                    
                } catch (Exception e) {
                    logger.error("Ошибка при отправке напоминания для бронирования #{}: {}", 
                                booking.getId(), e.getMessage());
                }
            }
        }
        
        if (reminderCount > 0) {
            logger.info("Отправлено {} напоминаний о предстоящих заездах", reminderCount);
        }
    }
    
    /**
     * Еженедельный отчет по бронированиям
     * Выполняется каждый понедельник в 08:00
     */
    @Scheduled(cron = "0 0 8 * * MON") // Каждый понедельник в 08:00
    public void generateWeeklyReport() {
        logger.info("Генерация еженедельного отчета: {}", LocalDateTime.now());
        
        try {
            LocalDate weekAgo = LocalDate.now().minusDays(7);
            LocalDate today = LocalDate.now();
            
            // Получаем бронирования за последнюю неделю
            List<Booking> recentBookings = bookingRepository.findAll().stream()
                .filter(b -> !b.getCreatedAt().isBefore(weekAgo))
                .toList();
            
            // Статистика
            long totalBookings = recentBookings.size();
            long pendingCount = recentBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING).count();
            long approvedCount = recentBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED).count();
            long rejectedCount = recentBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.REJECTED).count();
            
            logger.info("=== ЕЖЕНЕДЕЛЬНЫЙ ОТЧЕТ ===");
            logger.info("Период: {} - {}", weekAgo, today);
            logger.info("Всего бронирований: {}", totalBookings);
            logger.info("Ожидающие: {}", pendingCount);
            logger.info("Одобренные: {}", approvedCount);
            logger.info("Отклоненные: {}", rejectedCount);
            logger.info("========================");
            
        } catch (Exception e) {
            logger.error("Ошибка при генерации еженедельного отчета: {}", e.getMessage());
        }
    }
    
    /**
     * Вспомогательный метод для проверки пересечения дат
     */
    private boolean datesOverlap(LocalDate start1, LocalDate end1, 
                                 LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
}