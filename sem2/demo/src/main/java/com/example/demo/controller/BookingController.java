package com.example.demo.controller;

import com.example.demo.model.Booking;
import com.example.demo.model.User;
import com.example.demo.service.BookingService;
import com.example.demo.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    
    // Страница создания бронирования
    @GetMapping("/create")
    public String createBookingForm(@RequestParam Long roomId, Model model) {
        model.addAttribute("roomId", roomId);
        model.addAttribute("minDate", LocalDate.now().plusDays(1));
        model.addAttribute("maxDate", LocalDate.now().plusMonths(3));
        return "booking/create";
    }
    
    // Создание бронирования
    @PostMapping("/create")
    public String createBooking(
            @RequestParam Long roomId,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
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
            return "redirect:/booking/create?roomId=" + roomId;
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
        
        // Проверяем доступ - ИСПРАВЛЕННАЯ СТРОКА 61
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
    
    // Вспомогательные методы для проверки ролей
    private boolean isModerator(User user) {
        return user != null && 
               ("ROLE_MODERATOR".equals(user.getRole()) || "ROLE_ADMIN".equals(user.getRole()));
    }
    
    private boolean isAdmin(User user) {
        return user != null && "ROLE_ADMIN".equals(user.getRole());
    }
}