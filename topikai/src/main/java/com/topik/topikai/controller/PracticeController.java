package com.topik.topikai.controller;

import com.topik.topikai.service.UsageQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/practice")
@CrossOrigin(origins = "*")
public class PracticeController {

    @Autowired
    private UsageQuotaService usageQuotaService;

    @GetMapping("/can-use")
    public ResponseEntity<Map<String, Object>> canUse(
            @RequestParam Long userId,
            @RequestParam String featureKey
    ) {
        if (!usageQuotaService.isValidPracticeFeatureKey(featureKey)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "featureKey không hợp lệ",
                    "featureKey", featureKey
            ));
        }

        boolean canUse = usageQuotaService.canUsePractice(userId, featureKey);
        boolean premium = usageQuotaService.isPremium(userId);
        int used = usageQuotaService.countPracticeUsage(userId, featureKey);
        int limit = usageQuotaService.getPracticeLimit(featureKey, premium);

        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("featureKey", featureKey);
        body.put("canUse", canUse);
        body.put("isPremium", premium);
        body.put("used", used);
        body.put("limit", limit);
        body.put("periodDate", usageQuotaService.getPracticePeriodDate(featureKey).toString());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/consume")
    public ResponseEntity<Map<String, Object>> consume(@RequestBody ConsumeRequest request) {
        if (request.getUserId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu userId"));
        }
        if (request.getFeatureKey() == null || request.getFeatureKey().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu featureKey"));
        }
        if (!usageQuotaService.isValidPracticeFeatureKey(request.getFeatureKey())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "featureKey không hợp lệ",
                    "featureKey", request.getFeatureKey()
            ));
        }

        Long userId = request.getUserId();
        String featureKey = request.getFeatureKey();
        boolean premium = usageQuotaService.isPremium(userId);

        if (!usageQuotaService.canUsePractice(userId, featureKey)) {
            Map<String, Object> denied = new HashMap<>();
            denied.put("quotaExceeded", true);
            denied.put("message", "Hết lượt miễn phí. Nâng cấp PREMIUM để luyện không giới hạn.");
            denied.put("userId", userId);
            denied.put("featureKey", featureKey);
            denied.put("used", usageQuotaService.countPracticeUsage(userId, featureKey));
            denied.put("limit", usageQuotaService.getPracticeLimit(featureKey, false));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(denied);
        }

        usageQuotaService.consumePractice(userId, featureKey);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("userId", userId);
        body.put("featureKey", featureKey);
        body.put("isPremium", premium);
        body.put("used", usageQuotaService.countPracticeUsage(userId, featureKey));
        body.put("limit", usageQuotaService.getPracticeLimit(featureKey, premium));
        body.put("canUse", usageQuotaService.canUsePractice(userId, featureKey));
        return ResponseEntity.ok(body);
    }

    public static class ConsumeRequest {
        private Long userId;
        private String featureKey;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getFeatureKey() {
            return featureKey;
        }

        public void setFeatureKey(String featureKey) {
            this.featureKey = featureKey;
        }
    }
}
