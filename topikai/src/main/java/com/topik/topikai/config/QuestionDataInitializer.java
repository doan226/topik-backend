package com.topik.topikai.config;

import com.topik.topikai.entity.WritingQuestion;
import com.topik.topikai.repository.WritingQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class QuestionDataInitializer implements ApplicationRunner {

    @Autowired
    private WritingQuestionRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) return;

        seed(3551, 35, 51, 150, 10,
                "무료로 드립니다.\n유학생인데 공부를 마치고 다음 주에 고향으로 돌아갑니다. 그래서 지금 저는 ( ㉠ ). 책상, 의자, 컴퓨터, 전공 책 등이 있습니다. 이번 주 금요일까지 ( ㉡ ). 연락 기다리겠습니다.",
                "㉠ 제가 쓰던 물건들을 정리하고 있습니다.\n㉡ 방을 비워 줘야 하기 때문입니다.",
                null);
        seed(3552, 35, 52, 150, 10,
                "사람들은 거짓말을 할 때 평소와 다른 행동을 한다. 그 중 하나가 코를 만지는 것이다.",
                "㉠ 붓기 때문이다.\n㉡ 만지게 되는 것이다.",
                null);
        seed(3553, 35, 53, 900, 30,
                "다음을 참고하여 '대학생의 취업 준비'에 대한 글을 200~300자로 쓰십시오.",
                "대학신문에서 대학생 1,000명을 대상으로 조사한 결과...",
                "/topik_images/topik35_53.png");
        seed(3651, 36, 51, 150, 10,
                "도서관 이용 안내\n우리 도서관을 이용해 주셔서 감사합니다.",
                "㉠ 할 예정입니다\n㉡ 이용하실 수 없습니다",
                null);
    }

    private void seed(int externalId, int topik, int type, int timeLimit, int maxScore,
                      String prompt, String answer, String imageUrl) {
        WritingQuestion q = new WritingQuestion();
        q.setExternalId(externalId);
        q.setTopik(topik);
        q.setType(type);
        q.setTimeLimit(timeLimit);
        q.setMaxScore(maxScore);
        q.setPrompt(prompt);
        q.setAnswer(answer);
        q.setImageUrl(imageUrl);
        repository.save(q);
    }
}
