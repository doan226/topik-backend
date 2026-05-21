package com.topik.topikai.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import com.topik.topikai.entity.User;
import com.topik.topikai.entity.Role;
import com.topik.topikai.entity.UserAnswer;
import com.topik.topikai.repository.UserAnswerRepository;
import com.topik.topikai.repository.UserRepository;
import com.topik.topikai.service.GeminiService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/topik")
@CrossOrigin(origins = "*")
public class SubmissionController {

    @Autowired
    private GeminiService aiService;

    @Autowired
    private UserAnswerRepository userAnswerRepository;

    @Autowired
    private UserRepository userRepository; // Bổ sung kho chứa User

    @PostMapping("/submit")
    public String submitWriting(@RequestBody SubmitRequest request) {

        // 1. Kiểm tra an ninh: Nếu đang nộp bài cho đề 52, phải chắc chắn đây là VIP
        if (request.getQuestionNumber() == 52) {
            Optional<User> userOptional = userRepository.findById(request.getUserId());
            if (userOptional.isEmpty() || userOptional.get().getRole() != Role.PREMIUM_USER) {
                return "{\"total_score\": 0, \"native_suggestion\": \"LỖI TỪ CHỐI TRUY CẬP: Bạn cần nâng cấp PREMIUM để AI chấm điểm đề 52.\"}";
            }
        }

        // 2. Chạy luồng chấm điểm bình thường nếu thỏa mãn điều kiện
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

class SubmitRequest {
    private String content;
    private int questionNumber;
    private Long userId;

    public String getContent() { return content; }
    public int getQuestionNumber() { return questionNumber; }
    public Long getUserId() { return userId; }
}