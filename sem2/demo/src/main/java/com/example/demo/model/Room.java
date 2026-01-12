package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String number;
    
    @Column(nullable = false)
    private String type;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private Double price = 0.0; // Измените на Double и добавьте значение по умолчанию
    
    @Column(nullable = false)
    private int capacity;
    
    @Column(nullable = false)
    private boolean available = true;
    
    // Конструкторы
    public Room() {}
    
    // Геттеры и сеттеры
    public Long getId() { return id; }
    public String getNumber() { return number; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public Double getPrice() { return price; }
    public int getCapacity() { return capacity; }
    public boolean isAvailable() { return available; }
    
    public void setId(Long id) { this.id = id; }
    public void setNumber(String number) { this.number = number; }
    public void setType(String type) { this.type = type; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(Double price) { this.price = price != null ? price : 0.0; } // Проверка на null
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setAvailable(boolean available) { this.available = available; }
}