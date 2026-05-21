package com.topik.topikai.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "users") // Giữ tên bảng là 'users' để tránh trùng từ khóa hệ thống
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // --- CÁC TRƯỜNG MỚI ĐỂ PHỤC VỤ XÁC THỰC EMAIL & ÔN TẬP ---
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean isVerified = false; // Mặc định tài khoản chưa xác thực

    private String verificationCode; // Lưu mã xác thực gửi qua email
    // -------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // FREE_USER hoặc PREMIUM_USER

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDate createdAt;
}