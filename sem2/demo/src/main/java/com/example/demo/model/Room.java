package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Номер комнаты обязателен")
    @Column(nullable = false, unique = true, length = 20)
    private String number;
    
    @NotBlank(message = "Тип комнаты обязателен")
    @Column(nullable = false, length = 50)
    private String type;
    
    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @NotNull(message = "Цена обязательна")
    @Min(value = 0, message = "Цена не может быть отрицательной")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;
    
    @NotNull(message = "Вместимость обязательна")
    @Min(value = 1, message = "Вместимость должна быть не менее 1 человека")
    @Column(nullable = false)
    private Integer capacity = 1;
    
    @Column(nullable = false)
    private Boolean available = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Конструкторы
    public Room() {}
    
    public Room(String number, String type, String description, BigDecimal price, Integer capacity) {
        this.number = number;
        this.type = type;
        this.description = description;
        this.price = price;
        this.capacity = capacity;
    }
    
    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    
    public Boolean isAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Преобразование типа для отображения
    public String getDisplayType() {
        return switch (type) {
            case "STANDARD" -> "Стандарт";
            case "VIP" -> "VIP";
            case "DELUXE" -> "Делюкс";
            case "SUITE" -> "Люкс";
            case "FAMILY" -> "Семейный";
            case "EXECUTIVE" -> "Бизнес";
            default -> type;
        };
    }
}