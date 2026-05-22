package com.topik.topikai.controller;

import com.topik.topikai.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GeminiService geminiService;

    // 🎯 API LẤY LỊCH SỬ HỌC TẬP TOÀN DIỆN
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserHistory(@PathVariable Long userId) {
        try {
            // Lấy chính xác trường question_number để phân loại câu 51, 52, 53, 54
            // ORDER BY created_at DESC -> Database sẽ trả về danh sách có các bài làm MỚI NHẤT nằm ở đầu
            String sql = "SELECT id, question_number, content, ai_feedback_json, created_at " +
                    "FROM user_answer WHERE user_id = ? ORDER BY created_at DESC";

            List<Map<String, Object>> rawHistory = jdbcTemplate.queryForList(sql, userId);

            // ===================================================================================
            // 💡 THUẬT TOÁN XỬ LÝ LỌC TOP 10 VÀ ĐẢO NGƯỢC CHIỀU THỜI GIAN
            // ===================================================================================
            List<Map<String, Object>> processedHistory = new ArrayList<>();
            int count51 = 0, count52 = 0, count53 = 0, count54 = 0;

            // 1. Chạy qua từng bài làm (Đang từ mới đến cũ)
            for (Map<String, Object> record : rawHistory) {
                // Ép kiểu an toàn để lấy số câu hỏi
                int qNum = ((Number) record.get("question_number")).intValue();

                // 2. Chỉ nhặt tối đa 10 bài cho mỗi loại câu hỏi
                if (qNum == 51 && count51 < 10) { processedHistory.add(record); count51++; }
                else if (qNum == 52 && count52 < 10) { processedHistory.add(record); count52++; }
                else if (qNum == 53 && count53 < 10) { processedHistory.add(record); count53++; }
                else if (qNum == 54 && count54 < 10) { processedHistory.add(record); count54++; }
            }

            // 3. Đảo ngược lại toàn bộ danh sách đã lọc.
            // Lúc này bài CŨ nhất sẽ lên đầu, bài MỚI nhất xuống cuối.
            // Khi gửi sang React vẽ biểu đồ từ Trái sang Phải sẽ đi đúng theo chiều thời gian tiến tới.
            Collections.reverse(processedHistory);
            // ===================================================================================

            return ResponseEntity.ok(processedHistory);

        } catch (Exception e) {
            System.err.println("🔴 LỖI TRUY XUẤN DASHBOARD: " + e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi truy xuất dữ liệu: " + e.getMessage());
        }
    }

    // 🎯 API TẠO BÀI TẬP KHẮC PHỤC CÁ NHÂN HÓA QUA AI
    @PostMapping("/generate-test")
    public ResponseEntity<?> generateTest(@RequestBody Map<String, String> payload) {
        try {
            String errorHistory = payload.get("errorHistory");
            String testJson = geminiService.analyzeErrorsAndGenerateTest(errorHistory);
            return ResponseEntity.ok(testJson);
        } catch (Exception e) {
            System.err.println("🔴 LỖI TẠO BÀI TẬP: " + e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi tạo bài tập: " + e.getMessage());
        }
    }
}