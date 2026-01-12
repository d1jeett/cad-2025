package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Отключаем CSRF для упрощения (для production лучше включить)
            .csrf(csrf -> csrf.disable())
            
            // Настройка авторизации запросов
            .authorizeHttpRequests(auth -> auth
                // Публичные пути (доступны всем)
                .requestMatchers("/", "/login", "/register", "/css/**", 
                               "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/rooms", "/rooms/**").permitAll()
                .requestMatchers("/api/availability/**").permitAll()
                
                // Пользовательские пути (только для авторизованных)
                .requestMatchers("/booking/create", "/booking/my", "/booking/cancel/**", 
                               "/booking/details/**").hasAnyRole("USER", "MODERATOR", "ADMIN")
                
                // Модераторские пути
                .requestMatchers("/rooms/create", "/rooms/edit/**", "/rooms/manage",
                               "/booking/pending", "/booking/approve/**", "/booking/reject/**")
                    .hasAnyRole("MODERATOR", "ADMIN")
                
                // Админские пути
                .requestMatchers("/admin/**", "/rooms/delete/**").hasRole("ADMIN")
                
                // Все остальные запросы требуют аутентификации
                .anyRequest().authenticated()
            )
            
            // Настройка формы входа
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            
            // Настройка выхода
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            
            // Обработка ошибок доступа
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedPage("/access-denied")
            );
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}