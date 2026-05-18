package com.topik.topikai.controller;

import com.topik.topikai.entity.Role;
import com.topik.topikai.entity.User;
import com.topik.topikai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. API ĐĂNG KÝ TÀI KHOẢN (Mặc định là FREE_USER)
    @PostMapping("/register")
    public Map<String, Object> registerUser(@RequestBody AuthRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (userRepository.existsByUsername(request.getUsername())) {
            response.put("success", false);
            response.put("message", "Tên đăng nhập đã tồn tại!");
            return response;
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        // Mã hóa mật khẩu trước khi lưu vào DB
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(Role.FREE_USER);

        userRepository.save(newUser);

        response.put("success", true);
        response.put("message", "Đăng ký thành công!");
        return response;
    }

    // 2. API ĐĂNG NHẬP
    @PostMapping("/login")
    public Map<String, Object> loginUser(@RequestBody AuthRequest request) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // So sánh mật khẩu người dùng nhập vào với mật khẩu đã mã hóa trong DB
            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                response.put("success", true);
                response.put("message", "Đăng nhập thành công!");
                response.put("userId", user.getId());
                response.put("role", user.getRole().name());
                return response;
            }
        }

        response.put("success", false);
        response.put("message", "Sai tên đăng nhập hoặc mật khẩu!");
        return response;
    }
}

// Lớp DTO hứng dữ liệu từ React
class AuthRequest {
    private String username;
    private String password;
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}