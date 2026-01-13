package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // Разрешаем доступ без аутентификации
                .requestMatchers(
                    "/",                     // главная страница
                    "/login",                // страница логина
                    "/logout",               // выход
                    "/register",             // регистрация
                    "/api/**",               // все API
                    "/h2-console/**",        // H2 консоль
                    "/css/**",               // стили
                    "/js/**",                // скрипты
                    "/images/**",            // изображения
                    "/swagger-ui/**",        // Swagger
                    "/v3/api-docs/**"        // OpenAPI
                ).permitAll()
                .anyRequest().authenticated() // всё остальное требует аутентификации
            )
            .formLogin(form -> form
                .loginPage("/login")          // кастомная страница логина
                .loginProcessingUrl("/login") // URL для обработки логина
                .defaultSuccessUrl("/")       // редирект после успешного входа
                .failureUrl("/login?error=true") // редирект при ошибке
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .headers(headers -> headers
                .frameOptions().disable() // Для H2 консоли
            );

        return http.build();
    }
}