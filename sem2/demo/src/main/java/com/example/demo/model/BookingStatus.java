package com.example.demo.model;

public enum BookingStatus {
    PENDING("Ожидает"),
    APPROVED("Подтверждено"),
    REJECTED("Отклонено"),
    CANCELLED("Отменено"),
    COMPLETED("Завершено");
    
    private final String displayName;
    
    BookingStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}