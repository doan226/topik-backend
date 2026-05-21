package com.topik.topikai.controller;

import com.topik.topikai.entity.User;
import com.topik.topikai.entity.Role;
import com.topik.topikai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/exams")
@CrossOrigin(origins = "*")
public class ExamController {

    @Autowired
    private UserRepository userRepository;

    // Frontend bây giờ chỉ cần gửi userId lên, ví dụ: /api/v1/exams/52?userId=1
    @GetMapping("/{examId}")
    public Map<String, Object> getExam(@PathVariable int examId, @RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();

        // Logic 1: Nhóm đề miễn phí (Đề 1 đến Đề 5) -> Cho phép tải luôn không cần check VIP
        if (examId >= 1 && examId <= 5) {
            response.put("success", true);
            response.put("message", "Đang tải nội dung Đề " + examId + "... Chúc bạn làm bài tốt!");
            return response;
        }

        // Logic 2: Nhóm đề nâng cấp (Đề 52)
        if (examId == 52) {
            // Chọc vào Database để lấy thông tin user thật
            Optional<User> userOptional = userRepository.findById(userId);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                // Kiểm tra bằng enum Role chuẩn xác
                if (user.getRole() == Role.PREMIUM_USER) {
                    response.put("success", true);
                    response.put("message", "✨ Chào mừng VIP! Đang tải nội dung Đề 52 độc quyền...");
                } else {
                    response.put("success", false);
                    response.put("message", "🔒 Đề 52 là tài liệu PREMIUM. Vui lòng nâng cấp tài khoản để sử dụng!");
                }
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy thông tin người dùng!");
            }
            return response;
        }

        // Nếu nhập sai số đề không tồn tại
        response.put("success", false);
        response.put("message", "Đề thi không tồn tại trên hệ thống!");
        return response;
    }
}