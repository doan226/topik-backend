package com.topik.topikai.controller;

import com.topik.topikai.service.InteractiveExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exams/interactive")
@CrossOrigin(origins = "*")
public class InteractiveExamController {

    @Autowired
    private InteractiveExamService interactiveExamService;

    @GetMapping("/questions")
    public ResponseEntity<Map<String, Object>> questions(
            @RequestParam String section,
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "topik2-91") String examId) {
        List<Map<String, Object>> items = interactiveExamService.listQuestions(section, examId, userId);
        return ResponseEntity.ok(Map.of("success", true, "questions", items));
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submit(
            @RequestParam Long userId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(interactiveExamService.submit(userId, body));
    }

    @PostMapping("/ai-explain")
    public ResponseEntity<Map<String, Object>> aiExplain(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(interactiveExamService.aiExplain(body));
    }
}
