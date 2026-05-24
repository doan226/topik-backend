package com.topik.topikai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

@Service
public class GeminiService {

    private static final String MODEL_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    @Value("${gemini.api.key:}")
    private String geminiApiKeyFromProps;

    private String resolveApiKey() {
        if (geminiApiKeyFromProps != null && !geminiApiKeyFromProps.isBlank()) {
            return geminiApiKeyFromProps.trim();
        }
        String env = System.getenv("GEMINI_API_KEY");
        return env != null ? env.trim() : "";
    }

    private String buildApiUrl() {
        return MODEL_URL + resolveApiKey();
    }

    public String gradeTopikWriting(String studentText, int questionType) {
        String key = resolveApiKey();
        if (key.isEmpty()) {
            return "{\"total_score\": 0, \"native_suggestion\": \"⚠️ Chưa cấu hình GEMINI_API_KEY. Thêm biến môi trường hoặc gemini.api.key trong application.properties.\"}";
        }

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

    public String generateWritingQuestionSet(int topikSession, String excludeTopics) {
        String key = resolveApiKey();
        if (key.isEmpty()) {
            return "{\"error\":\"Chưa cấu hình GEMINI_API_KEY.\"}";
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String exclude = excludeTopics != null && !excludeTopics.isBlank()
                ? excludeTopics
                : "취업, 1인 가구, 스마트폰, 교사, 외국인 유학생, 인구, 지도자, 직업, 대중문화, 역사, 과정, 의사소통, 제주도, 스트레스";

        String systemPrompt = "Bạn là chuyên gia biên soạn đề thi TOPIK II (쓰기). "
                + "Sinh BỘ 4 CÂU HỎI VIẾT cho kỳ thi TOPIK số " + topikSession + ".\n"
                + "CHỈ TRẢ VỀ JSON, KHÔNG MARKDOWN, KHÔNG GIẢI THÍCH.\n"
                + "Tránh trùng chủ đề với các đề sau: " + exclude + ".\n\n"
                + "Quy tắc:\n"
                + "- Câu 51: hoàn thành câu ngắn, có ( ㉠ ) và ( ㉡ ), timeLimit=150, maxScore=10\n"
                + "- Câu 52: hoàn thành đoạn văn logic, có ( ㉠ ) và ( ㉡ ), timeLimit=150, maxScore=10\n"
                + "- Câu 53: mô tả biểu đồ/số liệu 200~300 chữ, kèm số liệu trong prompt, timeLimit=900, maxScore=30, imageUrl=null\n"
                + "- Câu 54: nghị luận 600~700 chữ với 3 gợi ý con, timeLimit=3000, maxScore=50\n"
                + "- prompt và answer bằng tiếng Hàn (answer câu 54 có thể thêm 1 dòng ghi chú tiếng Việt ngắn)\n"
                + "- externalId = topik * 100 + type\n\n"
                + "JSON BẮT BUỘC:\n"
                + "{\n  \"questions\": [\n    {\n      \"externalId\": " + (topikSession * 100 + 51) + ",\n"
                + "      \"topik\": " + topikSession + ",\n      \"type\": 51,\n"
                + "      \"timeLimit\": 150,\n      \"maxScore\": 10,\n"
                + "      \"prompt\": \"...\",\n      \"answer\": \"...\",\n      \"imageUrl\": null\n    }\n  ]\n}";

        return callRealGeminiApi(restTemplate, headers, systemPrompt, false);
    }

    public String analyzeErrorsAndGenerateTest(String errorHistory) {
        String key = resolveApiKey();
        if (key.isEmpty()) {
            return "{\"main_weakness\": \"Chưa cấu hình API\", \"analysis\": \"Thiếu GEMINI_API_KEY.\", \"mini_test\": []}";
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemPrompt = "Bạn là chuyên gia giáo dục ngôn ngữ Hàn. TẠO 3 CÂU TRẮC NGHIỆM TỪ LỖI SAI SAU ĐÂY:\n" + errorHistory +
                "\nCHỈ TRẢ VỀ JSON, KHÔNG DÙNG MARKDOWN VĂN BẢN THỪA:\n" +
                "{\n  \"main_weakness\": \"...\",\n  \"analysis\": \"...\",\n  \"mini_test\": [\n    {\n      \"question\": \"...\",\n      \"options\": [\"A. ...\", \"B. ...\", \"C. ...\", \"D. ...\"],\n      \"correct_answer\": \"...\",\n      \"explanation\": \"...\"\n    }\n  ]\n}";

        return callRealGeminiApi(restTemplate, headers, systemPrompt, false);
    }

    private String callRealGeminiApi(RestTemplate restTemplate, HttpHeaders headers, String systemPrompt, boolean isGrading) {
        try {
            JSONObject jsonBody = new JSONObject();

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

            ResponseEntity<String> response = restTemplate.postForEntity(buildApiUrl(), request, String.class);
            JSONObject jsonObj = new JSONObject(response.getBody());

            String rawText = jsonObj.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");

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
                return "{\"total_score\": 0, \"native_suggestion\": \"⚠️ Google API lỗi (" + e.getStatusCode() + "). Kiểm tra GEMINI_API_KEY!\" }";
            } else {
                return "{\"main_weakness\": \"Lỗi kết nối API\", \"analysis\": \"⚠️ Google API lỗi (" + e.getStatusCode() + ").\", \"mini_test\": [] }";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"total_score\": 0, \"native_suggestion\": \"⚠️ Lỗi Java: " + e.getMessage() + "\"}";
        }
    }
}
