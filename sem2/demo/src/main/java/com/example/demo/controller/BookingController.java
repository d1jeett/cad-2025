package com.example.demo.controller;

import com.example.demo.model.Booking;
import com.example.demo.model.User;
import com.example.demo.service.BookingService;
import com.example.demo.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/booking")
public class BookingController {
    
    private final BookingService bookingService;
    private final UserService userService;
    
    public BookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }
    
    // Страница создания бронирования (обновленная с параметрами)
    @GetMapping("/create")
    public String createBookingForm(@RequestParam Long roomId,
                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkIn,
                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkOut,
                                  Model model) {
        
        model.addAttribute("roomId", roomId);
        
        // Устанавливаем минимальную и максимальную даты
        LocalDate minDate = LocalDate.now().plusDays(1);
        LocalDate maxDate = LocalDate.now().plusMonths(3);
        model.addAttribute("minDate", minDate);
        model.addAttribute("maxDate", maxDate);
        
        // Если даты переданы, используем их
        if (checkIn != null && checkOut != null) {
            // Проверяем валидность дат
            if (checkIn.isBefore(minDate) || checkOut.isBefore(minDate)) {
                model.addAttribute("error", "Даты должны быть не раньше " + minDate);
            } else if (checkIn.isAfter(checkOut) || checkIn.equals(checkOut)) {
                model.addAttribute("error", "Дата заезда должна быть раньше даты выезда");
            } else {
                model.addAttribute("checkIn", checkIn);
                model.addAttribute("checkOut", checkOut);
                
                // Проверяем доступность комнаты
                try {
                    boolean isAvailable = bookingService.isRoomAvailableForDates(roomId, checkIn, checkOut);
                    if (!isAvailable) {
                        model.addAttribute("error", "Комната недоступна на выбранные даты");
                    } else {
                        // Рассчитываем стоимость (возвращает BigDecimal)
                        BigDecimal totalPrice = bookingService.calculateTotalPrice(roomId, checkIn, checkOut);
                        model.addAttribute("totalPrice", totalPrice);
                        model.addAttribute("isAvailable", true);
                    }
                } catch (IllegalArgumentException e) {
                    model.addAttribute("error", e.getMessage());
                }
            }
        }
        
        return "booking/create";
    }
    
    // Создание бронирования (оставляем как было)
    @PostMapping("/create")
    public String createBooking(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkIn,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkOut,
            @RequestParam String guestName,
            @RequestParam String guestEmail,
            @RequestParam(required = false) String specialRequests,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        
        try {
            Booking booking = bookingService.createBooking(
                roomId,
                user.getId(),
                checkIn,
                checkOut,
                guestName,
                guestEmail,
                specialRequests
            );
            
            redirectAttributes.addFlashAttribute("success", 
                "Бронирование создано успешно! Номер бронирования: " + booking.getId());
            return "redirect:/booking/my";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/booking/create?roomId=" + roomId + 
                   "&checkIn=" + checkIn + 
                   "&checkOut=" + checkOut;
        }
    }
    
    // Мои бронирования
    @GetMapping("/my")
    public String myBookings(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("bookings", bookingService.getUserBookings(user.getId()));
        return "booking/my";
    }
    
    // Детали бронирования
    @GetMapping("/{id}")
    public String bookingDetails(@PathVariable Long id, 
                                 @AuthenticationPrincipal User user,
                                 Model model) {
        Booking booking = bookingService.getBookingById(id);
        
        // Проверяем доступ
        if (!booking.getUser().getId().equals(user.getId()) && 
            !("ROLE_MODERATOR".equals(user.getRole()) || "ROLE_ADMIN".equals(user.getRole()))) {
            return "redirect:/access-denied";
        }
        
        model.addAttribute("booking", booking);
        model.addAttribute("canCancel", booking.canBeCancelled());
        return "booking/details";
    }
    
    // Страница ожидающих бронирований (для модератора)
    @GetMapping("/pending")
    public String pendingBookings(@AuthenticationPrincipal User user, Model model) {
        // Проверка прав - добавлена проверка пользователя
        if (!("ROLE_MODERATOR".equals(user.getRole()) || "ROLE_ADMIN".equals(user.getRole()))) {
            return "redirect:/access-denied";
        }
        
        model.addAttribute("bookings", bookingService.getPendingBookings());
        return "booking/pending";
    }
    
    // Одобрение бронирования
    @GetMapping("/approve/{id}")
    public String approveBooking(@PathVariable Long id, 
                                 @AuthenticationPrincipal User user,
                                 RedirectAttributes redirectAttributes) {
        // Проверка прав
        if (!("ROLE_MODERATOR".equals(user.getRole()) || "ROLE_ADMIN".equals(user.getRole()))) {
            return "redirect:/access-denied";
        }
        
        try {
            bookingService.approveBooking(id);
            redirectAttributes.addFlashAttribute("success", "Бронирование одобрено");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/booking/pending";
    }
    
    // Отклонение бронирования
    @GetMapping("/reject/{id}")
    public String rejectBooking(@PathVariable Long id,
                                @AuthenticationPrincipal User user,
                                RedirectAttributes redirectAttributes) {
        // Проверка прав
        if (!("ROLE_MODERATOR".equals(user.getRole()) || "ROLE_ADMIN".equals(user.getRole()))) {
            return "redirect:/access-denied";
        }
        
        try {
            bookingService.rejectBooking(id);
            redirectAttributes.addFlashAttribute("success", "Бронирование отклонено");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/booking/pending";
    }
    
    // Отмена бронирования пользователем
    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable Long id,
                                @AuthenticationPrincipal User user,
                                RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelBooking(id, user.getId());
            redirectAttributes.addFlashAttribute("success", "Бронирование отменено");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/booking/my";
    }
    
    // Все бронирования (для администратора)
    @GetMapping("/all")
    public String allBookings(@AuthenticationPrincipal User user, Model model) {
        // Проверка прав - только админ
        if (!"ROLE_ADMIN".equals(user.getRole())) {
            return "redirect:/access-denied";
        }
        
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "booking/all";
    }
    
    // Упрощенная форма быстрого бронирования
    @GetMapping("/quick")
    public String quickBooking(@RequestParam Long roomId,
                             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkIn,
                             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkOut,
                             @AuthenticationPrincipal User user,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        
        try {
            // Проверяем доступность
            boolean isAvailable = bookingService.isRoomAvailableForDates(roomId, checkIn, checkOut);
            
            if (!isAvailable) {
                redirectAttributes.addFlashAttribute("error", "Комната недоступна на выбранные даты");
                return "redirect:/rooms/" + roomId;
            }
            
            // Автозаполняем данные пользователя
            model.addAttribute("roomId", roomId);
            model.addAttribute("checkIn", checkIn);
            model.addAttribute("checkOut", checkOut);
            model.addAttribute("guestName", user.getUsername());
            model.addAttribute("guestEmail", user.getEmail());
            
            // Рассчитываем стоимость (возвращает BigDecimal)
            BigDecimal totalPrice = bookingService.calculateTotalPrice(roomId, checkIn, checkOut);
            model.addAttribute("totalPrice", totalPrice);
            
            return "booking/quick";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/rooms/" + roomId;
        }
    }
    
    // Вспомогательные методы для проверки ролей
    private boolean isModerator(User user) {
        return user != null && 
               ("ROLE_MODERATOR".equals(user.getRole()) || "ROLE_ADMIN".equals(user.getRole()));
    }
    
    private boolean isAdmin(User user) {
        return user != null && "ROLE_ADMIN".equals(user.getRole());
    }
}