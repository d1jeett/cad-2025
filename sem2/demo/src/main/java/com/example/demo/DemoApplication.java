package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner startSwingClient() {
        return args -> {
            System.out.println("\n========================================");
            System.out.println("Spring Boot запущен!");
            System.out.println("Сайт: http://localhost:8080");
            System.out.println("========================================\n");
            
            // Просто показываем инструкцию
            System.out.println("Для запуска Swing клиента выполни в отдельном окне:");
            System.out.println("cd swing-client");
            System.out.println("java -cp \"json.jar;.\" Main");
            System.out.println("\nИли запусти файл: swing-client\\run.bat");
        };
    }
}