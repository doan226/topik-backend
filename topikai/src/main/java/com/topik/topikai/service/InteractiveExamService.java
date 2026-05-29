package com.topik.topikai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topik.topikai.entity.ExamQuestion;
import com.topik.topikai.entity.ExamSubmission;
import com.topik.topikai.entity.Role;
import com.topik.topikai.repository.ExamQuestionRepository;
import com.topik.topikai.repository.ExamSubmissionRepository;
import com.topik.topikai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class InteractiveExamService {

    @Autowired
    private ExamQuestionRepository questionRepository;

    @Autowired
    private ExamSubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GeminiService geminiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> listQuestions(String section, String examId, Long userId) {
        // Nghe + Đọc đều miễn phí cho mọi user — không lọc theo tier.
        List<ExamQuestion> questions = examId == null || examId.isBlank()
                ? questionRepository.findBySectionOrderBySortOrderAsc(section)
                : questionRepository.findByExamIdAndSectionOrderBySortOrderAsc(examId, section);

        return questions.stream()
                .map(this::toQuestionDto)
                .toList();
    }

    @Transactional
    public Map<String, Object> submit(Long userId, Map<String, Object> body) {
        Long questionId = longVal(body.get("questionId"));
        String userAnswer = str(body.get("userAnswer"));
        if (questionId == null || userAnswer.isBlank()) {
            return Map.of("success", false, "message", "Thiếu questionId hoặc userAnswer");
        }
        ExamQuestion q = questionRepository.findById(questionId).orElse(null);
        if (q == null) {
            return Map.of("success", false, "message", "Không tìm thấy câu hỏi");
        }
        boolean correct = userAnswer.trim().equalsIgnoreCase(q.getCorrectAns().trim());

        ExamSubmission sub = new ExamSubmission();
        sub.setUserId(userId);
        sub.setQuestionId(questionId);
        sub.setUserAnswer(userAnswer);
        sub.setIsCorrect(correct);
        submissionRepository.save(sub);

        return Map.of(
                "success", true,
                "isCorrect", correct,
                "correctAnswer", q.getCorrectAns()
        );
    }

    public Map<String, Object> aiExplain(Map<String, Object> body) {
        String passage = str(body.get("passage"));
        String question = str(body.get("question"));
        String userAnswer = str(body.get("userAnswer"));
        String correctAnswer = str(body.get("correctAnswer"));

        String prompt = "Bạn là giáo viên TOPIK II. Giải thích ngắn gọn bằng tiếng Việt tại sao đáp án đúng là "
                + correctAnswer + " và vì sao học viên chọn " + userAnswer + ".\n"
                + "Đoạn văn: " + passage + "\nCâu hỏi: " + question;

        String raw = geminiService.gradeTopikWriting(prompt, 52);
        return Map.of("success", true, "explanation", raw);
    }

    private Map<String, Object> toQuestionDto(ExamQuestion q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId());
        m.put("exam_id", q.getExamId());
        m.put("section", q.getSection());
        m.put("question_no", q.getQuestionNo());
        m.put("correct_ans", q.getCorrectAns());
        m.put("tier", q.getTier());
        try {
            m.put("content_json", objectMapper.readValue(q.getContentJson(), new TypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            m.put("content_json", Map.of());
        }
        return m;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static Long longVal(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
