package com.project_microservice.discovery_server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Value("${spring.security.user.name}")
    private String username;

    @Value("${spring.security.user.password}")
    private String password;

    // Cấu hình Security Filter Chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Vô hiệu hóa CSRF (nếu cần, tùy thuộc vào ứng dụng của bạn)
                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/eureka/**").permitAll()
                        .anyRequest().authenticated()// Yêu cầu xác thực cho các yêu cầu khác
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    // Cấu hình UserDetailsManager với in-memory authentication
    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username(username) // Tên đăng nhập
                .password(password) // Mật khẩu đã được mã hóa
                .roles("USER") // Phân quyền
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    // Cấu hình PasswordEncoder để mã hóa mật khẩu
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance(); // Sử dụng BCryptPasswordEncoder để mã hóa mật khẩu
    }
}
