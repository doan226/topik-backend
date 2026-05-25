package com.topik.topikai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Mở khóa CORS cho Security
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Mở khóa toàn bộ các API liên quan đến thanh toán VNPay
                        .requestMatchers("/api/v1/payment/**").permitAll()
                        // 1. Mở cửa tự do cho Đăng nhập, Đăng ký, Xác nhận OTP và các API công khai
                        .requestMatchers("/api/v1/auth/**", "/api/v1/topik/**", "/api/v1/dashboard/**", "/api/v1/questions/**", "/api/v1/practice/**", "/api/v1/project/**", "/api/v1/hanja/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").permitAll()

                        // 2. BƯỚC 4 - PHÂN QUYỀN: Chỉ những User có Role là PREMIUM mới được vào đường dẫn này
                        .requestMatchers("/api/v1/premium-features/**").hasAuthority("PREMIUM")

                        // 3. Tất cả các đường dẫn khác đều bắt buộc phải đăng nhập mới được xem
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    // Cấu hình cho phép React gọi API mà không bị chặn
    // Cấu hình cho phép React gọi API chuẩn hóa bảo mật
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Chỉ đích danh các nguồn được phép truy cập dữ liệu hệ thống
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",                     // React khi chạy dưới máy local
                "https://topik-frontend-red.vercel.app"      // Tên miền Vercel thật của bạn trên mạng
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // Bây giờ dòng này đã hợp pháp vì domain đã rõ ràng

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}