package com.topik.topikai.controller;

import com.topik.topikai.entity.Role;
import com.topik.topikai.entity.User;
import com.topik.topikai.repository.UserRepository;
import com.topik.topikai.service.EmailService;
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

    @Autowired
    private EmailService emailService;

    // 1. API ĐĂNG KÝ TÀI KHOẢN (Mặc định là FREE_USER, isVerified = false)
    @PostMapping("/register")
    public Map<String, Object> registerUser(@RequestBody AuthRequest request) {
        System.out.println("=======> BACKEND ĐÃ NHẬN REQUEST! Email: " + request.getEmail() + " | User: " + request.getUsername());
        Map<String, Object> response = new HashMap<>();

        if (userRepository.existsByUsername(request.getUsername())) {
            response.put("success", false);
            response.put("message", "Tên đăng nhập đã tồn tại!");
            return response;
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            response.put("success", false);
            response.put("message", "Email này đã được sử dụng!");
            return response;
        }

        String code = String.valueOf((int)(Math.random() * 900000) + 100000);

        try {
            emailService.sendVerificationEmail(request.getEmail(), code);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Không thể gửi mail! Chi tiết lỗi:");
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Lỗi gửi email xác thực: " + e.getMessage());
            return response;
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setEmail(request.getEmail());
        newUser.setVerificationCode(code);
        newUser.setVerified(false);
        newUser.setRole(Role.FREE_USER);

        userRepository.save(newUser);

        response.put("success", true);
        response.put("message", "Đăng ký thành công! Vui lòng kiểm tra email.");
        return response;
    }

    // 2. API ĐĂNG NHẬP
    @PostMapping("/login")
    public Map<String, Object> loginUser(@RequestBody AuthRequest request) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
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

    // 3. API XÁC THỰC MÃ OTP (Mới được bổ sung để kết nối với React)
    @PostMapping("/verify")
    public Map<String, Object> verifyUser(@RequestBody VerifyRequest request) {
        Map<String, Object> response = new HashMap<>();

        // Tìm kiếm user theo tên đăng nhập
        Optional<User> userOptional = userRepository.findByUsername(request.getUsername());

        if (userOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "Tài khoản không tồn tại trên hệ thống!");
            return response;
        }

        User user = userOptional.get();

        // Kiểm tra xem mã OTP người dùng nhập vào có khớp với mã lưu trong Database không
        if (user.getVerificationCode() != null && user.getVerificationCode().equals(request.getCode())) {
            user.setVerified(true);         // Kích hoạt tài khoản thành công
            user.setVerificationCode(null);  // Xóa mã OTP cũ đi để bảo mật
            userRepository.save(user);      // Lưu lại thay đổi xuống DB

            response.put("success", true);
            response.put("message", "Xác thực tài khoản thành công!");
        } else {
            response.put("success", false);
            response.put("message", "Mã xác thực không chính xác hoặc đã hết hạn!");
        }

        return response;
    }
}

// Lớp DTO hứng dữ liệu Đăng nhập / Đăng ký
class AuthRequest {
    private String username;
    private String password;
    private String email;

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
}

// Lớp DTO mới hứng dữ liệu xác thực OTP từ React gửi lên
class VerifyRequest {
    private String username;
    private String code;

    public void setUsername(String username) { this.username = username; }
    public void setCode(String code) { this.code = code; }

    public String getUsername() { return username; }
    public String getCode() { return code; }
}