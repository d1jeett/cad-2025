package com.example.demo.controller;

import com.example.demo.model.Booking;
import com.example.demo.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/my-bookings")  // ИЗМЕНИЛ ПУТЬ!
@CrossOrigin(origins = "*")
public class BookingController {
    
    @Autowired
    private BookingService bookingService;
    
    // 1. Получить бронирования по email
    @GetMapping("/email")
    public ResponseEntity<List<Booking>> getBookingsByEmail(@RequestParam String email) {
        try {
            List<Booking> bookings = bookingService.getBookingsByAnyEmail(email);
            return ResponseEntity.ok(bookings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // 2. Получить бронирования по email гостя
    @GetMapping("/guest-email")
    public ResponseEntity<List<Booking>> getBookingsByGuestEmail(@RequestParam String email) {
        try {
            List<Booking> bookings = bookingService.getBookingsByGuestEmail(email);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}