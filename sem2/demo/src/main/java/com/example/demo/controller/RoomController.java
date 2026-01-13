package com.example.demo.controller;

import com.example.demo.model.Booking;
import com.example.demo.model.Room;
import com.example.demo.model.User;
import com.example.demo.model.BookingStatus;
import com.example.demo.service.BookingService;
import com.example.demo.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
@RequestMapping("/rooms")
public class RoomController {
    
    private final RoomService roomService;
    private final BookingService bookingService;
    
    @Autowired
    public RoomController(RoomService roomService, BookingService bookingService) {
        this.roomService = roomService;
        this.bookingService = bookingService;
    }
    
    /**
     * Страница создания комнаты (только для модератора и админа)
     */
    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String showCreateRoomForm(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("roomTypes", new String[]{
            "STANDARD", "VIP", "DELUXE", "SUITE", "FAMILY", "EXECUTIVE"
        });
        return "rooms/create";
    }
    
    /**
     * Создание новой комнаты
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String createRoom(@ModelAttribute("room") Room room,
                           @AuthenticationPrincipal User user,
                           RedirectAttributes redirectAttributes) {
        
        try {
            // Устанавливаем значения по умолчанию
            if (room.getPrice() == null || room.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                room.setPrice(new BigDecimal("1000.00"));
            }
            if (room.getCapacity() == null || room.getCapacity() <= 0) {
                room.setCapacity(2);
            }
            if (room.getAvailable() == null) {
                room.setAvailable(true);
            }
            
            roomService.saveRoom(room);
            
            redirectAttributes.addFlashAttribute("success", 
                "Комната №" + room.getNumber() + " успешно создана!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Ошибка при создании комнаты: " + e.getMessage());
            return "redirect:/rooms/create";
        }
        
        return "redirect:/";
    }
    
    /**
     * Детальная информация о комнате (доступно всем)
     */
    @GetMapping("/{id}")
    public String roomDetails(@PathVariable Long id, 
                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkIn,
                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkOut,
                            Model model) {
        
        try {
            Room room = roomService.getRoomById(id);
            model.addAttribute("room", room);
            
            // Бронирования для этой комнаты
            List<Booking> bookings = bookingService.getBookingsByRoom(id);
            model.addAttribute("bookings", bookings);
            
            // Статус доступности
            LocalDate today = LocalDate.now();
            model.addAttribute("today", today);
            model.addAttribute("minDate", today.plusDays(1));
            model.addAttribute("maxDate", today.plusMonths(3));
            
            // Проверка доступности на конкретные даты
            if (checkIn != null && checkOut != null) {
                boolean isAvailable = bookingService.isRoomAvailableForDates(id, checkIn, checkOut);
                model.addAttribute("checkIn", checkIn);
                model.addAttribute("checkOut", checkOut);
                model.addAttribute("isAvailableForDates", isAvailable);
                
                if (isAvailable) {
                    BigDecimal totalPrice = bookingService.calculateTotalPrice(id, checkIn, checkOut);
                    model.addAttribute("totalPrice", totalPrice);
                    model.addAttribute("duration", 
                        ChronoUnit.DAYS.between(checkIn, checkOut));
                }
            }
            
            // Статистика по комнате
            long totalBookings = bookings.size();
            long approvedBookings = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED)
                .count();
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("approvedBookings", approvedBookings);
            
            return "rooms/details";
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }
    
    /**
     * Форма редактирования комнаты
     */
    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String showEditRoomForm(@PathVariable Long id, Model model) {
        try {
            Room room = roomService.getRoomById(id);
            model.addAttribute("room", room);
            model.addAttribute("roomTypes", new String[]{
                "STANDARD", "VIP", "DELUXE", "SUITE", "FAMILY", "EXECUTIVE"
            });
            return "rooms/edit";
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }
    
    /**
     * Обновление информации о комнате
     */
    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String updateRoom(@PathVariable Long id,
                           @ModelAttribute("room") Room roomDetails,
                           @AuthenticationPrincipal User user,
                           RedirectAttributes redirectAttributes) {
        
        try {
            Room existingRoom = roomService.getRoomById(id);
            
            // Обновляем поля
            existingRoom.setNumber(roomDetails.getNumber());
            existingRoom.setType(roomDetails.getType());
            existingRoom.setDescription(roomDetails.getDescription());
            existingRoom.setPrice(roomDetails.getPrice());
            existingRoom.setCapacity(roomDetails.getCapacity());
            existingRoom.setAvailable(roomDetails.getAvailable());
            
            roomService.saveRoom(existingRoom);
            
            redirectAttributes.addFlashAttribute("success", 
                "Комната №" + existingRoom.getNumber() + " успешно обновлена!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Ошибка при обновлении комнаты: " + e.getMessage());
            return "redirect:/rooms/edit/" + id;
        }
        
        return "redirect:/rooms/" + id;
    }
    
    /**
     * Удаление комнаты (только для админа)
     */
    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteRoom(@PathVariable Long id,
                           @AuthenticationPrincipal User user,
                           RedirectAttributes redirectAttributes) {
        try {
            Room room = roomService.getRoomById(id);
            
            // Проверяем, есть ли активные бронирования
            List<Booking> bookings = bookingService.getBookingsByRoom(id);
            boolean hasActiveBookings = bookings.stream()
                .anyMatch(b -> b.getStatus() == BookingStatus.APPROVED || 
                             b.getStatus() == BookingStatus.PENDING);
            
            if (hasActiveBookings) {
                redirectAttributes.addFlashAttribute("error", 
                    "Невозможно удалить комнату: есть активные бронирования");
                return "redirect:/rooms/" + id;
            }
            
            roomService.deleteRoom(id);
            redirectAttributes.addFlashAttribute("success", 
                "Комната №" + room.getNumber() + " удалена");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
            return "redirect:/rooms/" + id;
        }
        
        return "redirect:/";
    }
    
    /**
     * Список всех комнат (для модератора и админа)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String allRooms(Model model) {
        List<Room> rooms = roomService.getAllRooms();
        model.addAttribute("rooms", rooms);
        return "rooms/all";
    }
    
    /**
     * Поиск комнат по номеру
     */
    @GetMapping("/search")
    public String searchRooms(@RequestParam String query, Model model) {
        List<Room> allRooms = roomService.getAllRooms();
        List<Room> rooms = allRooms.stream()
            .filter(room -> room.getNumber().toLowerCase().contains(query.toLowerCase()))
            .toList();
        
        model.addAttribute("rooms", rooms);
        model.addAttribute("searchQuery", query);
        return "rooms/search";
    }
    
    /**
     * API: Проверка доступности комнаты на даты
     */
    @GetMapping("/{id}/availability")
    @ResponseBody
    public AvailabilityResponse checkAvailability(@PathVariable Long id,
                                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkIn,
                                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkOut) {
        
        try {
            boolean isAvailable = bookingService.isRoomAvailableForDates(id, checkIn, checkOut);
            BigDecimal price = BigDecimal.ZERO;
            
            if (isAvailable) {
                price = bookingService.calculateTotalPrice(id, checkIn, checkOut);
            }
            
            return new AvailabilityResponse(isAvailable, price);
            
        } catch (IllegalArgumentException e) {
            return new AvailabilityResponse(false, BigDecimal.ZERO);
        }
    }
    
    /**
     * Быстрое бронирование - проверка и переход к созданию
     */
    @GetMapping("/{id}/book")
    public String quickBookRoom(@PathVariable Long id,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkIn,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkOut,
                              RedirectAttributes redirectAttributes) {
        
        try {
            // Проверяем существование комнаты
            Room room = roomService.getRoomById(id);
            
            // Если даты не указаны, просто показываем страницу деталей
            if (checkIn == null || checkOut == null) {
                // Перенаправляем на страницу деталей комнаты
                return "redirect:/rooms/" + id;
            }
            
            // Проверяем доступность на даты
            boolean isAvailable = bookingService.isRoomAvailableForDates(id, checkIn, checkOut);
            
            if (isAvailable) {
                // Если доступно, перенаправляем на страницу создания бронирования
                return "redirect:/booking/create?roomId=" + id + 
                       "&checkIn=" + checkIn + 
                       "&checkOut=" + checkOut;
            } else {
                // Если недоступно, показываем ошибку на странице деталей
                redirectAttributes.addFlashAttribute("error", 
                    "Комната №" + room.getNumber() + " недоступна с " + 
                    checkIn + " по " + checkOut);
                return "redirect:/rooms/" + id;
            }
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }
    
    /**
     * Список комнат по типу
     */
    @GetMapping("/type/{type}")
    public String roomsByType(@PathVariable String type, Model model) {
        List<Room> allRooms = roomService.getAllRooms();
        List<Room> filteredRooms = allRooms.stream()
            .filter(room -> room.getType().equalsIgnoreCase(type))
            .toList();
        
        model.addAttribute("rooms", filteredRooms);
        model.addAttribute("roomType", type);
        return "rooms/by-type";
    }
    
    /**
     * Список доступных комнат на даты
     */
    @GetMapping("/available")
    public String availableRooms(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkIn,
                               @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkOut,
                               Model model) {
        
        try {
            List<Room> availableRooms = bookingService.getAvailableRoomsForDates(checkIn, checkOut);
            model.addAttribute("rooms", availableRooms);
            model.addAttribute("checkIn", checkIn);
            model.addAttribute("checkOut", checkOut);
            model.addAttribute("availableCount", availableRooms.size());
            
            return "rooms/available";
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }
    
    /**
     * Изменение статуса доступности комнаты
     */
    @PostMapping("/{id}/toggle-availability")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @ResponseBody
    public ToggleResponse toggleAvailability(@PathVariable Long id,
                                           @RequestParam boolean available,
                                           @AuthenticationPrincipal User user) {
        
        try {
            Room room = roomService.getRoomById(id);
            room.setAvailable(available);
            roomService.saveRoom(room);
            
            return new ToggleResponse(true, "Статус комнаты обновлен");
            
        } catch (Exception e) {
            return new ToggleResponse(false, "Ошибка: " + e.getMessage());
        }
    }
    
    /**
     * Страница управления комнатами для модератора
     */
    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public String manageRooms(Model model, @AuthenticationPrincipal User user) {
        List<Room> rooms = roomService.getAllRooms();
        model.addAttribute("rooms", rooms);
        
        // Статистика
        long totalRooms = rooms.size();
        long availableRooms = rooms.stream()
            .filter(room -> Boolean.TRUE.equals(room.getAvailable()))
            .count();
        
        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("occupiedRooms", totalRooms - availableRooms);
        
        return "rooms/manage";
    }
    
    /**
     * DTO для ответа о доступности
     */
    public static class AvailabilityResponse {
        private boolean available;
        private BigDecimal price;
        
        public AvailabilityResponse(boolean available, BigDecimal price) {
            this.available = available;
            this.price = price;
        }
        
        public boolean isAvailable() { return available; }
        public BigDecimal getPrice() { return price; }
        
        public void setAvailable(boolean available) { this.available = available; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }
    
    /**
     * DTO для ответа о переключении статуса
     */
    public static class ToggleResponse {
        private boolean success;
        private String message;
        
        public ToggleResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        
        public void setSuccess(boolean success) { this.success = success; }
        public void setMessage(String message) { this.message = message; }
    }
}