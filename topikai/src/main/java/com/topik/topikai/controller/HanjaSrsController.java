package com.topik.topikai.controller;

import com.topik.topikai.service.HanjaSrsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hanja/srs")
@CrossOrigin(origins = "*")
public class HanjaSrsController {

    @Autowired
    private HanjaSrsService hanjaSrsService;

    @GetMapping("/cards")
    public ResponseEntity<Map<String, Object>> getCards(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "due") String filter) {
        return ResponseEntity.ok(hanjaSrsService.getCards(userId, filter));
    }

    @PostMapping("/cards")
    public ResponseEntity<Map<String, Object>> createCard(
            @RequestParam Long userId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = hanjaSrsService.createCard(userId, body);
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        return ResponseEntity.status(ok ? 201 : 400).body(result);
    }

    @PostMapping("/reviews")
    public ResponseEntity<Map<String, Object>> review(
            @RequestParam Long userId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = hanjaSrsService.submitReview(userId, body);
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        return ResponseEntity.status(ok ? 200 : 400).body(result);
    }

    @PostMapping("/migrate")
    public ResponseEntity<Map<String, Object>> migrate(
            @RequestParam Long userId,
            @RequestBody List<Map<String, Object>> entries) {
        return ResponseEntity.ok(hanjaSrsService.migrateFromLocal(userId, entries));
    }

    @PostMapping("/ensure-hanja")
    public ResponseEntity<Map<String, Object>> ensureHanja(
            @RequestParam Long userId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = hanjaSrsService.ensureHanjaCard(
                userId,
                String.valueOf(body.getOrDefault("externalRef", "")),
                String.valueOf(body.getOrDefault("word", "")),
                String.valueOf(body.getOrDefault("meaning", ""))
        );
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        return ResponseEntity.status(ok ? 200 : 403).body(result);
    }
}
