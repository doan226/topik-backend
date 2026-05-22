package com.topik.topikai.controller;

import com.topik.topikai.entity.Role;
import com.topik.topikai.entity.User;
import com.topik.topikai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/payment")
public class CassoController {

    @Autowired
    private UserRepository userRepository;

    // 🎯 MÃ BẢO MẬT API-KEY LẤY TỪ CASSO (Secure-Token)
    private final String CASSO_SECURE_TOKEN = "ĐIỀN_MÃ_BẢO_MẬT_CỦA_BẠN_VÀO_ĐÂY";

    @PostMapping("/casso-webhook")
    public ResponseEntity<?> handleCassoWebhook(
            @RequestHeader(value = "secure-token", required = false) String secureToken,
            @RequestBody Map<String, Object> payload) {

        try {
            // 1. Kiểm tra lớp bảo vệ: Đảm bảo request này thực sự đến từ máy chủ Casso
            if (secureToken == null || !secureToken.equals(CASSO_SECURE_TOKEN)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sai mã bảo mật bảo vệ hệ thống");
            }

            // 2. Bóc tách dữ liệu: Casso gửi data dưới dạng mảng (List)
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) payload.get("data");

            if (transactions == null || transactions.isEmpty()) {
                return ResponseEntity.ok(Map.of("error", 0, "message", "Không có dữ liệu giao dịch"));
            }

            // 3. Duyệt qua từng giao dịch ngân hàng để kiểm tra
            for (Map<String, Object> tx : transactions) {
                String description = (String) tx.get("description");
                // Ép kiểu an toàn vì Casso có thể gửi số tiền dưới dạng Integer hoặc Double
                int amount = ((Number) tx.get("amount")).intValue();

                // 4. Cốt lõi hệ thống: Kiểm tra có đúng cú pháp TOPIKVIP và đủ 50k không
                if (description != null && description.toUpperCase().contains("TOPIKVIP") && amount >= 50000) {

                    // Thuật toán tách ID User ra khỏi đoạn chữ "NGUYEN VAN A CHUYEN TIEN TOPIKVIP 1"
                    String[] parts = description.toUpperCase().split("TOPIKVIP");
                    if (parts.length > 1) {
                        String idString = parts[1].trim().split("[^0-9]")[0]; // Lấy cụm số đầu tiên
                        Long userId = Long.parseLong(idString);

                        // 5. Cập nhật thẳng vào Database MySQL
                        Optional<User> userOpt = userRepository.findById(userId);
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            user.setRole(Role.PREMIUM_USER);
                            userRepository.save(user);
                            System.out.println("✅ [CASSO] ĐÃ NÂNG CẤP VIP THÀNH CÔNG CHO USER ID: " + userId);
                        }
                    }
                }
            }

            // Bắt buộc trả về JSON có {"error": 0} để báo Casso biết đã nhận thành công
            return ResponseEntity.ok(Map.of("error", 0, "message", "Đã xử lý Webhook hoàn tất"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống khi xử lý Webhook");
        }
    }
}