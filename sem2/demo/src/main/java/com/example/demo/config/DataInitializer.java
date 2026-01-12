package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    
    @Bean
    public CommandLineRunner initUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        
        return args -> {
            // Проверяем и создаем пользователей, если их нет
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("1234"));
                admin.setRole("ROLE_ADMIN");
                admin.setEmail("admin@hotel.com");
                admin.setFullName("Администратор системы");
                admin.setEnabled(true);
                userRepository.save(admin);
                System.out.println("✓ Администратор создан: admin / 1234");
            }
            
            if (userRepository.findByUsername("moderator").isEmpty()) {
                User moderator = new User();
                moderator.setUsername("moderator");
                moderator.setPassword(passwordEncoder.encode("1234"));
                moderator.setRole("ROLE_MODERATOR");
                moderator.setEmail("moderator@hotel.com");
                moderator.setFullName("Модератор отеля");
                moderator.setEnabled(true);
                userRepository.save(moderator);
                System.out.println("✓ Модератор создан: moderator / 1234");
            }
            
            if (userRepository.findByUsername("user").isEmpty()) {
                User user = new User();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("1234"));
                user.setRole("ROLE_USER");
                user.setEmail("user@hotel.com");
                user.setFullName("Обычный пользователь");
                user.setEnabled(true);
                userRepository.save(user);
                System.out.println("✓ Пользователь создан: user / 1234");
            }
            
            // Дополнительные пользователи для тестирования
            if (userRepository.findByUsername("guest1").isEmpty()) {
                User guest = new User();
                guest.setUsername("guest1");
                guest.setPassword(passwordEncoder.encode("1234"));
                guest.setRole("ROLE_USER");
                guest.setEmail("guest1@example.com");
                guest.setFullName("Александр Иванов");
                guest.setEnabled(true);
                userRepository.save(guest);
                System.out.println("✓ Тестовый пользователь создан: guest1 / 1234");
            }
            
            System.out.println("✓ Инициализация пользователей завершена");
        };
    }
}