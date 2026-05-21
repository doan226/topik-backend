package com.topik.topikai.controller;

import org.springframework.web.bind.annotation.CrossOrigin; // 1. NHỚ IMPORT DÒNG NÀY
import org.springframework.web.bind.annotation.RestController;
import com.topik.topikai.entity.UserAnswer;
import com.topik.topikai.repository.UserAnswerRepository;
import com.topik.topikai.service.GeminiService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/topik")
@CrossOrigin(origins = "*")
public class SubmissionController {


    @Autowired
    private GeminiService aiService;

    @Autowired
    private UserAnswerRepository userAnswerRepository;

    @PostMapping("/submit")
    public String submitWriting(@RequestBody SubmitRequest request) {
        String aiResult = aiService.gradeTopikWriting(request.getContent(), request.getQuestionNumber());

        try {
            String cleanJson = aiResult;
            int start = cleanJson.indexOf('{');
            int end = cleanJson.lastIndexOf('}');
            if (start != -1 && end != -1) {
                cleanJson = cleanJson.substring(start, end + 1);
            }

            JSONObject jsonObject = new JSONObject(cleanJson);
            int totalScore = jsonObject.optInt("total_score", 0);

            UserAnswer answer = new UserAnswer();
            // 🎯 THAY ĐỔI QUAN TRỌNG: Lấy ID thật từ React gửi lên thay vì fix cứng 1L
            answer.setUserId(request.getUserId());

            answer.setQuestionNumber(request.getQuestionNumber());
            answer.setContent(request.getContent());
            answer.setAiFeedbackJson(cleanJson);
            answer.setScore(totalScore);

            userAnswerRepository.save(answer);

            return cleanJson;

        } catch (Exception e) {
            System.err.println("Lỗi bóc tách điểm: " + e.getMessage());
            return "{\"total_score\": 0, \"criteria_scores\": {}, \"grammar_errors\": [], \"native_suggestion\": \"Lỗi bóc tách điểm.\"}";
        }
    }
}

// 🎯 Bổ sung thêm biến userId vào khung hứng dữ liệu
class SubmitRequest {
    private String content;
    private int questionNumber;
    private Long userId;

    public String getContent() { return content; }
    public int getQuestionNumber() { return questionNumber; }
    public Long getUserId() { return userId; }
}