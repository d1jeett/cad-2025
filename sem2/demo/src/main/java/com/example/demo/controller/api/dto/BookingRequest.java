package com.example.demo.controller.api.dto;

import java.time.LocalDate;

public class BookingRequest {
    private Long roomId;
    private Long userId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String guestName;
    private String guestEmail;
    private String specialRequests;
    
    // Конструкторы
    public BookingRequest() {}
    
    public BookingRequest(Long roomId, Long userId, LocalDate checkIn, LocalDate checkOut, 
                         String guestName, String guestEmail, String specialRequests) {
        this.roomId = roomId;
        this.userId = userId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.specialRequests = specialRequests;
    }
    
    // Геттеры
    public Long getRoomId() { return roomId; }
    public Long getUserId() { return userId; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getSpecialRequests() { return specialRequests; }
    
    // Сеттеры
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
}