package com.example.demo.controller.api;

import com.example.demo.controller.api.dto.BookingRequest;
import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import com.example.demo.model.Room;
import com.example.demo.service.BookingService;
import com.example.demo.service.RoomService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {
    
    private final RoomService roomService;
    private final BookingService bookingService;
    private final UserService userService;
    
    @Autowired
    public ApiController(RoomService roomService, 
                        BookingService bookingService,
                        UserService userService) {
        this.roomService = roomService;
        this.bookingService = bookingService;
        this.userService = userService;
    }
    
    @GetMapping("/test")
    public ResponseEntity<?> testApi() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "API работает!");
        response.put("timestamp", LocalDate.now().toString());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/rooms/available")
    public ResponseEntity<?> getAvailableRooms() {
        try {
            List<Room> rooms = roomService.getAvailableRooms();
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/rooms/{id}")
    public ResponseEntity<?> getRoomById(@PathVariable Long id) {
        try {
            Room room = roomService.getRoomById(id);
            return ResponseEntity.ok(room);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @GetMapping("/rooms/{id}/availability")
    public ResponseEntity<?> checkRoomAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkIn,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkOut) {
        
        try {
            boolean isAvailable = bookingService.isRoomAvailableForDates(id, checkIn, checkOut);
            BigDecimal price = bookingService.calculateTotalPrice(id, checkIn, checkOut);
            
            Map<String, Object> response = new HashMap<>();
            response.put("available", isAvailable);
            response.put("price", price);
            response.put("checkIn", checkIn);
            response.put("checkOut", checkOut);
            response.put("roomId", id);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping("/rooms/available/dates")
    public ResponseEntity<?> getAvailableRoomsForDates(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkIn,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkOut) {
        
        try {
            List<Room> rooms = bookingService.getAvailableRoomsForDates(checkIn, checkOut);
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        try {
            Booking booking = bookingService.createBooking(
                request.getRoomId(),
                request.getUserId(),
                request.getCheckIn(),
                request.getCheckOut(),
                request.getGuestName(),
                request.getGuestEmail(),
                request.getSpecialRequests()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping("/users/{userId}/bookings")
    public ResponseEntity<?> getUserBookings(@PathVariable Long userId) {
        try {
            List<Booking> bookings = bookingService.getUserBookings(userId);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/bookings/{id}")
    public ResponseEntity<?> getBookingById(@PathVariable Long id) {
        try {
            Booking booking = bookingService.getBookingById(id);
            return ResponseEntity.ok(booking);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long id,
            @RequestParam Long userId) {
        
        try {
            bookingService.cancelBooking(id, userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Бронирование успешно отменено");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping("/rooms/search")
    public ResponseEntity<?> searchRooms(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) BigDecimal maxPrice) {
        
        try {
            List<Room> allRooms = roomService.getAvailableRooms();
            List<Room> filteredRooms = allRooms;
            
            if (type != null) {
                final String finalType = type;
                filteredRooms = filteredRooms.stream()
                    .filter(room -> room.getType().equalsIgnoreCase(finalType))
                    .toList();
            }
            
            if (minCapacity != null) {
                filteredRooms = filteredRooms.stream()
                    .filter(room -> room.getCapacity() >= minCapacity)
                    .toList();
            }
            
            if (maxPrice != null) {
                filteredRooms = filteredRooms.stream()
                    .filter(room -> room.getPrice().compareTo(maxPrice) <= 0)
                    .toList();
            }
            
            return ResponseEntity.ok(filteredRooms);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/admin/rooms")
    public ResponseEntity<?> getAllRooms() {
        try {
            List<Room> rooms = roomService.getAllRooms();
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/admin/rooms")
    public ResponseEntity<?> createRoom(@RequestBody Room room) {
        try {
            Room savedRoom = roomService.saveRoom(room);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedRoom);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PutMapping("/admin/rooms/{id}")
    public ResponseEntity<?> updateRoom(
            @PathVariable Long id,
            @RequestBody Room roomDetails) {
        
        try {
            Room existingRoom = roomService.getRoomById(id);
            existingRoom.setNumber(roomDetails.getNumber());
            existingRoom.setType(roomDetails.getType());
            existingRoom.setDescription(roomDetails.getDescription());
            existingRoom.setPrice(roomDetails.getPrice());
            existingRoom.setCapacity(roomDetails.getCapacity());
            existingRoom.setAvailable(roomDetails.getAvailable());
            
            Room updatedRoom = roomService.saveRoom(existingRoom);
            return ResponseEntity.ok(updatedRoom);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @DeleteMapping("/admin/rooms/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        try {
            roomService.deleteRoom(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Комната успешно удалена");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping("/admin/bookings")
    public ResponseEntity<?> getAllBookings() {
        try {
            List<Booking> bookings = bookingService.getAllBookings();
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getSystemStats() {
        try {
            List<Room> rooms = roomService.getAllRooms();
            List<Booking> bookings = bookingService.getAllBookings();
            
            long totalRooms = rooms.size();
            long availableRooms = rooms.stream()
                .filter(room -> Boolean.TRUE.equals(room.getAvailable()))
                .count();
            
            long totalBookings = bookings.size();
            long activeBookings = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED || 
                           b.getStatus() == BookingStatus.PENDING)
                .count();
            
            BigDecimal revenue = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED)
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRooms", totalRooms);
            stats.put("availableRooms", availableRooms);
            stats.put("occupiedRooms", totalRooms - availableRooms);
            stats.put("totalBookings", totalBookings);
            stats.put("activeBookings", activeBookings);
            stats.put("pendingBookings", bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING).count());
            stats.put("revenue", revenue);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}