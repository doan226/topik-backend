package com.topik.topikai.controller;

import com.topik.topikai.entity.Role;
import com.topik.topikai.entity.User;
import com.topik.topikai.entity.UserAnswer;
import com.topik.topikai.repository.UserAnswerRepository;
import com.topik.topikai.repository.UserRepository;
import com.topik.topikai.service.GeminiService;
import com.topik.topikai.service.UsageQuotaService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
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
    private UserRepository userRepository;

    @Autowired
    private UsageQuotaService usageQuotaService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitWriting(@RequestBody SubmitRequest request) {
        Long userId = request.getUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu userId"));
        }

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy người dùng"));
        }

        User user = userOptional.get();
        boolean premium = user.getRole() == Role.PREMIUM_USER;

        if (request.getQuestionNumber() == 54 && !premium) {
            Map<String, Object> denied = new HashMap<>();
            denied.put("quotaExceeded", true);
            denied.put("total_score", 0);
            denied.put("native_suggestion", "Câu 54 dành cho PREMIUM. Vui lòng nâng cấp tài khoản.");
            return ResponseEntity.ok(denied);
        }

        if (!usageQuotaService.canGrade(userId)) {
            Map<String, Object> denied = new HashMap<>();
            denied.put("quotaExceeded", true);
            denied.put("total_score", 0);
            denied.put("native_suggestion", "FREE: đã hết " + UsageQuotaService.FREE_DAILY_GRADING + " lượt chấm AI hôm nay. Nâng cấp PREMIUM để không giới hạn.");
            return ResponseEntity.ok(denied);
        }

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
            answer.setUserId(userId);
            answer.setQuestionNumber(request.getQuestionNumber());
            answer.setContent(request.getContent());
            answer.setAiFeedbackJson(cleanJson);
            answer.setScore(totalScore);

            userAnswerRepository.save(answer);

            return ResponseEntity.ok(cleanJson);

        } catch (Exception e) {
            System.err.println("Lỗi bóc tách điểm: " + e.getMessage());
            return ResponseEntity.ok("{\"total_score\": 0, \"criteria_scores\": {}, \"grammar_errors\": [], \"native_suggestion\": \"Lỗi bóc tách điểm.\"}");
        }
    }
}

class SubmitRequest {
    private String content;
    private int questionNumber;
    private Long userId;
    private Integer topikSession;

    public String getContent() { return content; }
    public int getQuestionNumber() { return questionNumber; }
    public Long getUserId() { return userId; }
    public Integer getTopikSession() { return topikSession; }

    public void setContent(String content) { this.content = content; }
    public void setQuestionNumber(int questionNumber) { this.questionNumber = questionNumber; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setTopikSession(Integer topikSession) { this.topikSession = topikSession; }
}
