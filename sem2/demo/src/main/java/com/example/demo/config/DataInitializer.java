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
                admin.setEmail("admin@example.com");
                admin.setEnabled(true);
                userRepository.save(admin);
                System.out.println("✓ Admin создан: admin / 1234");
            }
            
            if (userRepository.findByUsername("moderator").isEmpty()) {
                User moderator = new User();
                moderator.setUsername("moderator");
                moderator.setPassword(passwordEncoder.encode("1234"));
                moderator.setRole("ROLE_MODERATOR");
                moderator.setEmail("moderator@example.com");
                moderator.setEnabled(true);
                userRepository.save(moderator);
                System.out.println("✓ Moderator создан: moderator / 1234");
            }
            
            if (userRepository.findByUsername("user").isEmpty()) {
                User user = new User();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("1234"));
                user.setRole("ROLE_USER");
                user.setEmail("user@example.com");
                user.setEnabled(true);
                userRepository.save(user);
                System.out.println("✓ User создан: user / 1234");
            }
        };
    }
}