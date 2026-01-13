package com.hotel.swing;

import javax.swing.*;

public class HotelApp {
    public static void main(String[] args) {
        // Запуск в потоке обработки событий
        SwingUtilities.invokeLater(() -> {
            try {
                // Устанавливаем системный стиль
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                
                // Можно установить русские шрифты
                UIManager.put("Button.font", new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));
                UIManager.put("Label.font", new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));
                UIManager.put("TextField.font", new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Создаем и показываем главное окно
            HotelMainWindow window = new HotelMainWindow();
            window.setVisible(true);
            
            System.out.println("========================================");
            System.out.println("Клиент системы бронирования отелей запущен");
            System.out.println("API сервер: http://localhost:8080");
            System.out.println("Для работы требуется запущенный Spring Boot сервер");
            System.out.println("========================================");
        });
    }
}