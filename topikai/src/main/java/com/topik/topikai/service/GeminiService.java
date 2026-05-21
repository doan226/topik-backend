package com.topik.topikai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.web.client.HttpStatusCodeException;

@Service
public class GeminiService {

    // 🎯 Nhớ dán API Key "AIzaSy..." của bạn vào đây
    private final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");

    // 🎯 SỬ DỤNG BẢN ỔN ĐỊNH: gemini-1.5-flash
    // Đổi sang mô hình gemini-1.0-pro để vượt qua bộ lọc phân vùng IP của Render
    // Đảm bảo dòng URL đầu file của bạn trông chính xác như thế này:
    private final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.0-pro:generateContent?key=" + GEMINI_API_KEY;

    // --- PHƯƠNG THỨC 1: CHẤM ĐIỂM ---
    public String gradeTopikWriting(String studentText, int questionType) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        int maxScore = (questionType == 51 || questionType == 52) ? 10 : (questionType == 53 ? 30 : 50);
        String criteria = (questionType == 51 || questionType == 52)
                ? "\"ngữ_pháp_và_từ_vựng\": 5, \"ý_nghĩa_ngữ_cảnh\": 5"
                : "\"nội_dung\": 10, \"tổ_chức\": 10, \"ngôn_ngữ\": 10";

        String systemPrompt = "Bạn là giám khảo TOPIK II. CHỈ TRẢ VỀ JSON, KHÔNG CÓ MARKDOWN.\n" +
                "Đang chấm Câu " + questionType + ". Điểm tối đa: " + maxScore + ".\n" +
                "Định dạng JSON BẮT BUỘC:\n" +
                "{\n  \"total_score\": <tổng điểm>,\n  \"criteria_scores\": {" + criteria + "},\n  \"grammar_errors\": [{\"sai\":\"...\", \"đúng\":\"...\", \"lý_do\":\"...\"}],\n  \"native_suggestion\": \"<bài mẫu>\"\n}\n\nBài làm: " + studentText;

        return callRealGeminiApi(restTemplate, headers, systemPrompt, true);
    }

    // --- PHƯƠNG THỨC 2: TẠO BÀI TẬP (ĐÃ KHÔI PHỤC) ---
    public String analyzeErrorsAndGenerateTest(String errorHistory) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemPrompt = "Bạn là chuyên gia giáo dục ngôn ngữ Hàn. TẠO 3 CÂU TRẮC NGHIỆM TỪ LỖI SAI SAU ĐÂY:\n" + errorHistory +
                "\nCHỈ TRẢ VỀ JSON, KHÔNG DÙNG MARKDOWN:\n" +
                "{\n  \"main_weakness\": \"...\",\n  \"analysis\": \"...\",\n  \"mini_test\": [\n    {\n      \"question\": \"...\",\n      \"options\": [\"A. ...\", \"B. ...\", \"C. ...\", \"D. ...\"],\n      \"correct_answer\": \"...\",\n      \"explanation\": \"...\"\n    }\n  ]\n}";

        return callRealGeminiApi(restTemplate, headers, systemPrompt, false);
    }

    // --- PHƯƠNG THỨC GỌI API CHUNG ---
    private String callRealGeminiApi(RestTemplate restTemplate, HttpHeaders headers, String systemPrompt, boolean isGrading) {
        try {
            JSONObject jsonBody = new JSONObject();
            //JSONObject generationConfig = new JSONObject();
            //generationConfig.put("responseMimeType", "application/json");
            //jsonBody.put("generationConfig", generationConfig);

            JSONArray contentsArray = new JSONArray();
            JSONObject partsObject = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject textObject = new JSONObject();

            textObject.put("text", systemPrompt);
            partsArray.put(textObject);
            partsObject.put("parts", partsArray);
            contentsArray.put(partsObject);
            jsonBody.put("contents", contentsArray);

            HttpEntity<String> request = new HttpEntity<>(jsonBody.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
            JSONObject jsonObj = new JSONObject(response.getBody());

            return jsonObj.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");

        } catch (HttpStatusCodeException e) {
            String errorDetail = e.getResponseBodyAsString();
            System.err.println("🔴 LỖI TỪ GOOGLE API: " + e.getStatusCode());
            System.err.println("🔴 CHI TIẾT LỖI: " + errorDetail);

            if (isGrading) {
                return "{\"total_score\": 0, \"native_suggestion\": \"⚠️ Google API lỗi (" + e.getStatusCode() + "). Kiểm tra console để xem chi tiết!\" }";
            } else {
                return "{\"main_weakness\": \"Lỗi kết nối API\", \"analysis\": \"⚠️ Google API lỗi (" + e.getStatusCode() + ").\", \"mini_test\": [] }";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"total_score\": 0, \"native_suggestion\": \"⚠️ Lỗi Java: " + e.getMessage() + "\"}";
        }
    }
}