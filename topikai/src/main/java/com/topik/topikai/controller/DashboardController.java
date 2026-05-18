package com.topik.topikai.controller;

import com.topik.topikai.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

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
            String sql = "SELECT id, question_number, content, ai_feedback_json, created_at " +
                    "FROM user_answer WHERE user_id = ? ORDER BY created_at DESC";

            List<Map<String, Object>> history = jdbcTemplate.queryForList(sql, userId);
            return ResponseEntity.ok(history);
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