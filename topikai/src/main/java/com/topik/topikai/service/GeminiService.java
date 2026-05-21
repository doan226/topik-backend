package com.topik.topikai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.web.client.HttpStatusCodeException;

@Service
public class GeminiService {

    // 🎯 Lấy API Key bảo mật hoàn toàn từ Environment của Render (Không lo bị quét khóa key)
    private final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");

    // 🎯 URL bản v1 chính thức gọi mô hình gemini-1.5-flash ổn định nhất (Đã xóa bỏ hoàn toàn xung đột Git)
    // ✅ ĐÚNG
    // 🎯 Sửa lại chính xác dòng này trên GitHub:
    private final String API_URL = "https://gateway.ai.cloudflare.com/v1/7d7a46e12368c858509ce153d919ad77/gemini-proxy/google-ai/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;

    // --- PHƯƠNG THỨC 1: CHẤM ĐIỂM ---
    public String gradeTopikWriting(String studentText, int questionType) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        int maxScore = (questionType == 51 || questionType == 52) ? 10 : (questionType == 53 ? 30 : 50);
        String criteria = (questionType == 51 || questionType == 52)
                ? "\"ngữ_pháp_và_từ_vựng\": 5, \"ý_nghĩa_ngữ_cảnh\": 5"
                : "\"nội_dung\": 10, \"tổ_chức\": 10, \"ngôn_ngữ\": 10";

        String systemPrompt = "Bạn là giám khảo TOPIK II. CHỈ TRẢ VỀ JSON, KHÔNG CÓ MARKDOWN VĂN BẢN THỪA.\n" +
                "Đang chấm Câu " + questionType + ". Điểm tối đa: " + maxScore + ".\n" +
                "Định dạng JSON BẮT BUỘC:\n" +
                "{\n  \"total_score\": <tổng điểm>,\n  \"criteria_scores\": {" + criteria + "},\n  \"grammar_errors\": [{\"sai\":\"...\", \"đúng\":\"...\", \"lý_do\":\"...\"}],\n  \"native_suggestion\": \"<bài mẫu>\"\n}\n\nBài làm: " + studentText;

        return callRealGeminiApi(restTemplate, headers, systemPrompt, true);
    }

    // --- PHƯƠNG THỨC 2: TẠO BÀI TẬP ---
    public String analyzeErrorsAndGenerateTest(String errorHistory) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemPrompt = "Bạn là chuyên gia giáo dục ngôn ngữ Hàn. TẠO 3 CÂU TRẮC NGHIỆM TỪ LỖI SAI SAU ĐÂY:\n" + errorHistory +
                "\nCHỈ TRẢ VỀ JSON, KHÔNG DÙNG MARKDOWN VĂN BẢN THỪA:\n" +
                "{\n  \"main_weakness\": \"...\",\n  \"analysis\": \"...\",\n  \"mini_test\": [\n    {\n      \"question\": \"...\",\n      \"options\": [\"A. ...\", \"B. ...\", \"C. ...\", \"D. ...\"],\n      \"correct_answer\": \"...\",\n      \"explanation\": \"...\"\n    }\n  ]\n}";

        return callRealGeminiApi(restTemplate, headers, systemPrompt, false);
    }

    // --- PHƯƠNG THỨC GỌI API CHUNG (ĐÃ FIX LỖI 400) ---
    private String callRealGeminiApi(RestTemplate restTemplate, HttpHeaders headers, String systemPrompt, boolean isGrading) {
        try {
            JSONObject jsonBody = new JSONObject();

            // Đóng gói nội dung prompt gửi đi sạch sẽ, không chứa generation_config gây lỗi cấu trúc
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

            String rawText = jsonObj.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");

            // 🛡️ BỘ LỌC BẢO HIỂM: Tự động bóc tách cắt bỏ dấu định dạng dạng ```json ... ``` nếu AI trả về thừa
            if (rawText.contains("```json")) {
                rawText = rawText.substring(rawText.indexOf("```json") + 7);
                if (rawText.contains("```")) {
                    rawText = rawText.substring(0, rawText.indexOf("```"));
                }
            } else if (rawText.contains("```")) {
                rawText = rawText.substring(rawText.indexOf("```") + 3);
                if (rawText.contains("```")) {
                    rawText = rawText.substring(0, rawText.indexOf("```"));
                }
            }
            return rawText.trim();

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