package com.topik.topikai.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exams")
@CrossOrigin(origins = "*")
public class ExamController {

    // API này nhận ID của đề thi và Role của người dùng truyền lên
    @GetMapping("/{examId}")
    public Map<String, Object> getExam(@PathVariable int examId, @RequestParam(defaultValue = "FREE_USER") String role) {
        Map<String, Object> response = new HashMap<>();

        // Logic 1: Nhóm đề miễn phí (Đề 1 đến Đề 5) -> Ai cũng được vào
        if (examId >= 1 && examId <= 5) {
            response.put("success", true);
            response.put("message", "Đang tải nội dung Đề " + examId + "... Chúc bạn làm bài tốt!");
            return response;
        }

        // Logic 2: Nhóm đề nâng cấp (Ví dụ: Đề 52)
        if (examId == 52) {
            if ("PREMIUM".equals(role)) {
                // Nếu là VIP thì cho phép đi tiếp
                response.put("success", true);
                response.put("message", "✨ Chào mừng VIP! Đang tải nội dung Đề 52 độc quyền...");
            } else {
                // Nếu là FREE thì chặn đứng tại đây
                response.put("success", false);
                response.put("message", "🔒 Đề 52 là tài liệu PREMIUM. Vui lòng nâng cấp tài khoản để sử dụng!");
            }
            return response;
        }

        // Nếu nhập sai số đề không tồn tại
        response.put("success", false);
        response.put("message", "Đề thi không tồn tại trên hệ thống!");
        return response;
    }
}
