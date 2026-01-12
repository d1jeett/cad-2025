@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            // Публичные пути
            .requestMatchers("/", "/login", "/register", "/css/**", 
                           "/js/**", "/images/**", "/webjars/**").permitAll()
            .requestMatchers("/rooms").permitAll()
            
            // Пользовательские пути
            .requestMatchers("/booking/create", "/booking/my", "/booking/cancel/**", 
                           "/booking/details/**").hasAnyRole("USER", "MODERATOR", "ADMIN")
            
            // Модераторские пути
            .requestMatchers("/rooms/create", "/rooms/edit/**", 
                           "/booking/pending", "/booking/approve/**", "/booking/reject/**")
                .hasAnyRole("MODERATOR", "ADMIN")
            
            // Админские пути
            .requestMatchers("/admin/**", "/rooms/delete/**").hasRole("ADMIN")
            
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/", true)
            .permitAll()
        )
        .logout(logout -> logout
            .logoutSuccessUrl("/login?logout=true")
            .permitAll()
        )
        .exceptionHandling(exceptions -> exceptions
            .accessDeniedPage("/access-denied")
        );
    
    return http.build();
}