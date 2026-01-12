package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull(message = "Дата заезда обязательна")
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;
    
    @NotNull(message = "Дата выезда обязательна")
    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;
    
    @NotBlank(message = "Имя гостя обязательно")
    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;
    
    @NotBlank(message = "Email гостя обязателен")
    @Email(message = "Некорректный email")
    @Column(name = "guest_email", nullable = false, length = 100)
    private String guestEmail;
    
    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;
    
    @Column(name = "created_at")
    private LocalDate createdAt = LocalDate.now();
    
    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    // Конструкторы
    public Booking() {}
    
    public Booking(Room room, User user, LocalDate checkInDate, LocalDate checkOutDate, 
                   String guestName, String guestEmail) {
        this.room = room;
        this.user = user;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
    }
    
    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
    
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    
    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }
    
    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
    
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    
    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
    
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    
    // Расчетные поля
    @Transient
    public Integer getDuration() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }
    
    @Transient
    public BigDecimal calculateTotalPrice() {
        if (room != null && room.getPrice() != null && getDuration() > 0) {
            return room.getPrice().multiply(BigDecimal.valueOf(getDuration()));
        }
        return BigDecimal.ZERO;
    }
    
    @PrePersist
    @PreUpdate
    private void calculatePrice() {
        this.totalPrice = calculateTotalPrice();
    }
    
    // Проверка активности бронирования
    public boolean isActive() {
        return status == BookingStatus.PENDING || status == BookingStatus.APPROVED;
    }
    
    // Проверка возможности отмены
    public boolean canBeCancelled() {
        return isActive() && checkInDate.isAfter(LocalDate.now().plusDays(1));
    }
}