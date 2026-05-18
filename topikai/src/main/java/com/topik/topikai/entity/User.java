package com.topik.topikai.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "users") // Đổi tên bảng thành 'users' để tránh trùng từ khóa hệ thống của MySQL
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // FREE_USER hoặc PREMIUM_USER

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDate createdAt;
}