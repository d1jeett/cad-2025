package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String number;

    @Column(nullable = false)
    private String type;

    private boolean available;

    private double price;
    private int capacity;

    public Room() {}

    public Room(String number, String type, double price, int capacity) {
        this.number = number;
        this.type = type;
        this.price = price;
        this.capacity = capacity;
        this.available = true;
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
}
